#!/usr/bin/env python3
"""Lightweight Java sanity checker for environments without a JDK.

1. Parses every Java file with tree-sitter and reports syntax errors.
2. Verifies that every `import com.closky.rpgmod...` resolves to an existing file / nested type.
3. Verifies that `ModClass.member(` / `ModClass.CONSTANT` references to mod classes exist
   (static-style access through the simple class name).

Usage: python3 tools/check_java.py [src/main/java]
"""
import os
import re
import sys
from collections import defaultdict

try:
    import tree_sitter_java as tsjava
    from tree_sitter import Language, Parser
except ImportError:  # pragma: no cover
    print("tree-sitter / tree-sitter-java not installed: pip install tree-sitter tree-sitter-java")
    sys.exit(2)

ROOT = sys.argv[1] if len(sys.argv) > 1 else "src/main/java"
BASE_PKG = "com.closky.rpgmod"

JAVA = Language(tsjava.language())
parser = Parser(JAVA)


def iter_java_files(root):
    for dirpath, _, files in os.walk(root):
        for f in files:
            if f.endswith(".java"):
                yield os.path.join(dirpath, f)


def syntax_errors(tree, source):
    errors = []
    stack = [tree.root_node]
    while stack:
        node = stack.pop()
        if node.type == "ERROR" or node.is_missing:
            line = node.start_point[0] + 1
            snippet = source[node.start_byte:node.end_byte][:60].decode("utf8", "replace")
            errors.append((line, node.type, snippet))
        stack.extend(node.children)
    return errors


MEMBER_RE = re.compile(r"\b([A-Z][A-Za-z0-9_]*)\.([a-zA-Z_][A-Za-z0-9_]*)\b")


def collect_members(tree, source):
    """Returns {class_simple_name: set(member_names)} for all (nested) types in the file."""
    members = defaultdict(set)
    nested = defaultdict(set)

    def visit(node, owner):
        for child in node.children:
            t = child.type
            if t in ("class_declaration", "interface_declaration", "enum_declaration", "record_declaration"):
                name_node = child.child_by_field_name("name")
                name = source[name_node.start_byte:name_node.end_byte].decode()
                if owner:
                    nested[owner].add(name)
                    members[owner].add(name)
                members[name]  # ensure key
                body = child.child_by_field_name("body")
                if t == "record_declaration":
                    params = child.child_by_field_name("parameters")
                    if params:
                        for p in params.children:
                            if p.type == "formal_parameter":
                                pn = p.child_by_field_name("name")
                                members[name].add(source[pn.start_byte:pn.end_byte].decode())
                if body:
                    visit(body, name)
            elif t == "method_declaration" and owner:
                n = child.child_by_field_name("name")
                members[owner].add(source[n.start_byte:n.end_byte].decode())
            elif t == "field_declaration" and owner:
                for d in child.children:
                    if d.type == "variable_declarator":
                        n = d.child_by_field_name("name")
                        members[owner].add(source[n.start_byte:n.end_byte].decode())
            elif t == "enum_constant" and owner:
                n = child.child_by_field_name("name")
                members[owner].add(source[n.start_byte:n.end_byte].decode())
            elif t in ("enum_body", "enum_body_declarations", "class_body", "interface_body", "record_declaration_body") and owner:
                visit(child, owner)
            elif t == "constructor_declaration" and owner:
                members[owner].add("<init>")
    visit(tree.root_node, None)
    # supertypes (simple names) per declared type
    extends = {}
    stack = [tree.root_node]
    while stack:
        node = stack.pop()
        if node.type in ("class_declaration", "interface_declaration", "enum_declaration", "record_declaration"):
            name_node = node.child_by_field_name("name")
            name = source[name_node.start_byte:name_node.end_byte].decode()
            supers = []
            for c in node.children:
                if c.type in ("superclass", "super_interfaces", "extends_interfaces"):
                    txt = source[c.start_byte:c.end_byte].decode()
                    txt = re.sub(r"<[^<>]*(?:<[^<>]*>[^<>]*)*>", "", txt)
                    for part in re.split(r"[\s,]+", txt):
                        part = part.split(".")[-1]
                        if part and part[0].isupper():
                            supers.append(part)
            extends[name] = supers
        stack.extend(node.children)
    return members, nested, extends


def main():
    files = list(iter_java_files(ROOT))
    trees = {}
    all_members = {}
    all_nested = {}
    extends = {}
    class_files = {}
    had_error = False
    for path in files:
        with open(path, "rb") as fh:
            source = fh.read()
        tree = parser.parse(source)
        errs = syntax_errors(tree, source)
        if errs:
            had_error = True
            print(f"SYNTAX {path}")
            for line, kind, snippet in errs[:10]:
                print(f"   line {line}: {kind} {snippet!r}")
        trees[path] = (tree, source)
        members, nested, ext = collect_members(tree, source)
        for k, v in members.items():
            all_members.setdefault(k, set()).update(v)
        for k, v in nested.items():
            all_nested.setdefault(k, set()).update(v)
        extends.update(ext)
        rel = os.path.relpath(path, ROOT)[:-5].replace(os.sep, ".")
        class_files[rel] = path

    # import resolution
    for path, (tree, source) in trees.items():
        text = source.decode("utf8", "replace")
        for m in re.finditer(r"^import\s+(static\s+)?(" + re.escape(BASE_PKG) + r"[\w.]*?)(\.\*)?;", text, re.M):
            fq = m.group(2)
            if fq in class_files:
                continue
            parts = fq.split(".")
            # nested type or static member: parent must exist
            parent = ".".join(parts[:-1])
            if parent in class_files:
                simple = parts[-2]
                if parts[-1] in all_members.get(simple, set()) or parts[-1] in all_nested.get(simple, set()):
                    continue
            if m.group(3) and fq in {p.rsplit(".", 1)[0] for p in class_files}:
                continue
            had_error = True
            print(f"IMPORT {path}: unresolved {fq}")

    def lookup(cls, member, seen=None):
        """True if member is declared in cls or a mod supertype; None if unknown (non-mod supertype)."""
        seen = seen or set()
        if cls in seen:
            return False
        seen.add(cls)
        if member in all_members.get(cls, set()):
            return True
        unknown = False
        for sup in extends.get(cls, []):
            if sup in all_members:
                r = lookup(sup, member, seen)
                if r:
                    return True
                if r is None:
                    unknown = True
            else:
                unknown = True
        return None if unknown else False

    # static-style member access on mod classes
    mod_simple = {p.rsplit(".", 1)[-1] for p in class_files}
    for path, (tree, source) in trees.items():
        text = source.decode("utf8", "replace")
        # strip comments and strings crudely
        stripped = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
        stripped = re.sub(r"//[^\n]*", "", stripped)
        stripped = re.sub(r'"(?:\\.|[^"\\])*"', '""', stripped)
        local_types = set()
        for m in re.finditer(r"\b(?:class|interface|enum|record)\s+([A-Z]\w*)", stripped):
            local_types.add(m.group(1))
        # visible mod classes: same package, explicit mod imports (incl. nested types), local types
        rel = os.path.relpath(path, ROOT)[:-5].replace(os.sep, ".")
        pkg = rel.rsplit(".", 1)[0]
        visible = set(local_types)
        for other in class_files:
            if other.rsplit(".", 1)[0] == pkg:
                visible.add(other.rsplit(".", 1)[-1])
        for m in re.finditer(r"^import\s+(?:static\s+)?(" + re.escape(BASE_PKG) + r"[\w.]*?)(\.\*)?;", text, re.M):
            fq = m.group(1)
            if m.group(2):
                for other in class_files:
                    if other.rsplit(".", 1)[0] == fq:
                        visible.add(other.rsplit(".", 1)[-1])
                if fq in class_files:
                    visible |= all_nested.get(fq.rsplit(".", 1)[-1], set())
            else:
                visible.add(fq.rsplit(".", 1)[-1])
        for m in MEMBER_RE.finditer(stripped):
            cls, member = m.group(1), m.group(2)
            if cls not in visible:
                continue
            if cls not in all_members:
                continue
            if cls in extends:
                continue  # may be inherited
            if member in ("class", "this", "super", "values", "valueOf", "ordinal", "name"):
                continue
            prefix = stripped[max(0, m.start() - 1):m.start()]
            if prefix == ".":
                continue
            if lookup(cls, member) is False:
                line = stripped.count("\n", 0, m.start()) + 1
                had_error = True
                print(f"MEMBER {path}:{line}: {cls}.{member} not found")
        # instance member access through variables declared with a mod type
        decls = []  # (position, var, type) - any declaration, mod-typed or not
        for dm in re.finditer(r"\b([A-Z]\w*)(?:<[^>]*>)?(?:\[\])?\s+([a-z]\w*)\s*(?=[=;,)\:])", stripped):
            decls.append((dm.start(), dm.group(2), dm.group(1)))
        for dm in re.finditer(r"instanceof\s+([A-Z]\w*)\s+([a-z]\w*)", stripped):
            decls.append((dm.start(), dm.group(2), dm.group(1)))
        for dm in re.finditer(r"\bvar\s+([a-z]\w*)\s*=", stripped):
            decls.append((dm.start(), dm.group(1), "?"))
        decls.sort()
        for vm in re.finditer(r"\b([a-z]\w*)\.([a-zA-Z_]\w*)\s*\(", stripped):
            var, member = vm.group(1), vm.group(2)
            cls = None
            for pos, v, t in decls:
                if pos > vm.start():
                    break
                if v == var:
                    cls = t
            if not cls or cls not in visible or cls not in all_members:
                continue
            if member in ("equals", "hashCode", "toString", "getClass", "ordinal", "name", "compareTo"):
                continue
            if lookup(cls, member) is False:
                line = stripped.count("\n", 0, vm.start()) + 1
                had_error = True
                print(f"INSTANCE {path}:{line}: {var}({cls}).{member}() not found")

    print(f"checked {len(files)} files")
    sys.exit(1 if had_error else 0)


if __name__ == "__main__":
    main()
