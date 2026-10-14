#!/usr/bin/env python3
"""Heuristic scan for javac *Flow*-phase errors (which javac does not report while attribution errors exist).

Checks:
  1. lambdas / anonymous classes capturing local variables that are not effectively final
  2. unreachable statements after return / throw / break / continue in the same block
  3. non-void methods whose body may complete normally (missing return)
  4. local variables declared without an initializer (listed for manual definite-assignment review)
  5. calls to well-known checked-exception APIs outside try blocks / throwing methods

Run from the repository root: python3 tools/flow_scan.py [paths...]
"""
import pathlib
import sys

import tree_sitter_java
from tree_sitter import Language, Parser

JAVA = Language(tree_sitter_java.language())
PARSER = Parser(JAVA)

CHECKED_CALLS = {
    "parseForBlock": "CommandSyntaxException",
    "parseForTesting": "CommandSyntaxException",
    "parseTag": "CommandSyntaxException",
    "readCompressed": "IOException",
    "writeCompressed": "IOException",
    "readAllBytes": "IOException",
    "createDirectories": "IOException",
    "newBufferedReader": "IOException",
    "newBufferedWriter": "IOException",
    "newInputStream": "IOException",
    "newOutputStream": "IOException",
    "writeString": "IOException",
    "readString": "IOException",
    "sleep": "InterruptedException",
    "forName": "ClassNotFoundException",
    "newInstance": "ReflectiveOperationException",
    "getDeclaredField": "NoSuchFieldException",
    "getDeclaredMethod": "NoSuchMethodException",
    "invoke": "ReflectiveOperationException",
    "deleteIfExists": "IOException",
    "walk": "IOException",
    "list": "IOException",
}

findings = []


def text(node):
    return node.text.decode()


def warn(path, node, msg):
    findings.append(f"{path}:{node.start_point[0] + 1}: {msg}")


def children_of_type(node, *types):
    return [c for c in node.children if c.type in types]


def iter_nodes(node):
    stack = [node]
    while stack:
        n = stack.pop()
        yield n
        stack.extend(reversed(n.children))


def enclosing(node, *types):
    n = node.parent
    while n is not None:
        if n.type in types:
            return n
        n = n.parent
    return None


# --------------------------------------------------------------------------- 1. effectively final captures
def declared_locals(method_body):
    """name -> list of declarator nodes (locals + params + for vars + catch params + pattern vars)."""
    result = {}
    for n in iter_nodes(method_body):
        if n.type == "local_variable_declaration":
            for d in children_of_type(n, "variable_declarator"):
                name = text(d.child_by_field_name("name"))
                result.setdefault(name, []).append((d, d.child_by_field_name("value") is not None))
        elif n.type == "enhanced_for_statement":
            name = text(n.child_by_field_name("name"))
            result.setdefault(name, []).append((n, True))
        elif n.type == "catch_formal_parameter":
            name = text(n.child_by_field_name("name"))
            result.setdefault(name, []).append((n, True))
        elif n.type == "resource":
            nm = n.child_by_field_name("name")
            if nm is not None:
                result.setdefault(text(nm), []).append((n, True))
    return result


def assignments(method_body):
    """name -> count of assignments (=, +=, ++, --) to a bare identifier."""
    counts = {}
    for n in iter_nodes(method_body):
        if n.type == "assignment_expression":
            left = n.child_by_field_name("left")
            if left.type == "identifier":
                counts[text(left)] = counts.get(text(left), 0) + 1
        elif n.type == "update_expression":
            ident = [c for c in n.children if c.type == "identifier"]
            if ident:
                counts[text(ident[0])] = counts.get(text(ident[0]), 0) + 1
    return counts


def method_params(method):
    names = {}
    params = method.child_by_field_name("parameters")
    if params is None:
        return names
    for p in iter_nodes(params):
        if p.type in ("formal_parameter", "spread_parameter"):
            nm = p.child_by_field_name("name")
            if nm is None:  # spread parameter
                ids = [c for c in p.children if c.type == "identifier"]
                nm = ids[-1] if ids else None
            if nm is not None:
                names[text(nm)] = p
    return names


def check_captures(path, root):
    for method in iter_nodes(root):
        if method.type not in ("method_declaration", "constructor_declaration"):
            continue
        body = method.child_by_field_name("body")
        if body is None:
            continue
        locals_ = declared_locals(body)
        params = method_params(method)
        assigns = assignments(body)
        # names that are NOT effectively final
        bad = set()
        for name, decls in locals_.items():
            n_assign = assigns.get(name, 0)
            for decl, has_init in decls:
                if has_init and n_assign > 0:
                    bad.add(name)
                elif not has_init and n_assign > 1:
                    bad.add(name)
        for name in params:
            if assigns.get(name, 0) > 0:
                bad.add(name)
        if not bad:
            continue
        for lam in iter_nodes(body):
            if lam.type not in ("lambda_expression", "class_body"):
                continue
            if lam.type == "class_body" and lam.parent.type != "object_creation_expression":
                continue
            lam_body = lam.child_by_field_name("body") if lam.type == "lambda_expression" else lam
            if lam_body is None:
                continue
            # locals declared inside the lambda shadow outer ones
            inner = set(declared_locals(lam_body).keys())
            if lam.type == "lambda_expression":
                p = lam.child_by_field_name("parameters")
                if p is not None:
                    for c in iter_nodes(p):
                        if c.type == "identifier":
                            inner.add(text(c))
            for ident in iter_nodes(lam_body):
                if ident.type != "identifier":
                    continue
                name = text(ident)
                if name not in bad or name in inner:
                    continue
                par = ident.parent
                # skip field accesses like foo.name and method names
                if par.type == "field_access" and par.child_by_field_name("field") == ident:
                    continue
                if par.type == "method_invocation" and par.child_by_field_name("name") == ident:
                    continue
                warn(path, ident, f"lambda/anonymous class captures non-effectively-final local '{name}'")
                break


# --------------------------------------------------------------------------- 2. unreachable statements
JUMPS = ("return_statement", "throw_statement", "break_statement", "continue_statement")


def check_unreachable(path, root):
    for block in iter_nodes(root):
        if block.type not in ("block", "switch_block_statement_group"):
            continue
        stmts = [c for c in block.children if c.type.endswith("_statement") or c.type in ("local_variable_declaration", "block")]
        for i, s in enumerate(stmts[:-1]):
            if s.type in JUMPS:
                warn(path, stmts[i + 1], "unreachable statement after " + s.type)


# --------------------------------------------------------------------------- 3. missing return
def can_complete_normally(stmt):
    t = stmt.type
    if t in ("return_statement", "throw_statement"):
        return False
    if t == "block":
        stmts = [c for c in stmt.children if c.is_named and c.type not in ("line_comment", "block_comment")]
        if not stmts:
            return True
        return can_complete_normally(stmts[-1])
    if t == "if_statement":
        alt = stmt.child_by_field_name("alternative")
        if alt is None:
            return True
        return can_complete_normally(stmt.child_by_field_name("consequence")) or can_complete_normally(alt)
    if t == "while_statement":
        cond = text(stmt.child_by_field_name("condition"))
        if cond.strip("() ") == "true":
            return any(n.type == "break_statement" for n in iter_nodes(stmt.child_by_field_name("body")))
        return True
    if t == "for_statement":
        if stmt.child_by_field_name("condition") is None:
            return any(n.type == "break_statement" for n in iter_nodes(stmt.child_by_field_name("body")))
        return True
    if t == "do_statement":
        cond = text(stmt.child_by_field_name("condition"))
        return cond.strip("() ") != "true"
    if t == "try_statement" or t == "try_with_resources_statement":
        body = stmt.child_by_field_name("body")
        fin = [c for c in stmt.children if c.type == "finally_clause"]
        if fin and not can_complete_normally(fin[0].children[-1]):
            return False
        if can_complete_normally(body):
            return True
        for c in stmt.children:
            if c.type == "catch_clause" and can_complete_normally(c.child_by_field_name("body")):
                return True
        return False
    if t == "switch_expression":  # arrow-form switch statement
        body = stmt.child_by_field_name("body")
        has_default = False
        for c in body.children:
            if c.type == "switch_rule":
                label = c.children[0]
                if "default" in text(label):
                    has_default = True
                rule_body = c.children[-1]
                if rule_body.type == "block" and not can_complete_normally(rule_body):
                    continue
                if rule_body.type == "throw_statement":
                    continue
                return True
            elif c.type == "switch_block_statement_group":
                return True  # colon-form: assume may fall out
        return not has_default
    if t == "synchronized_statement":
        return can_complete_normally(stmt.child_by_field_name("body"))
    if t == "labeled_statement":
        return True
    if t == "expression_statement":
        return True
    return True


def check_missing_return(path, root):
    for method in iter_nodes(root):
        if method.type != "method_declaration":
            continue
        ret = method.child_by_field_name("type")
        body = method.child_by_field_name("body")
        if body is None or ret is None or text(ret) == "void":
            continue
        if can_complete_normally(body):
            warn(path, method, f"method '{text(method.child_by_field_name('name'))}' may complete without return")


# --------------------------------------------------------------------------- 4. uninitialised locals
def check_uninitialised(path, root):
    for n in iter_nodes(root):
        if n.type == "local_variable_declaration":
            for d in children_of_type(n, "variable_declarator"):
                if d.child_by_field_name("value") is None:
                    warn(path, d, f"local '{text(d.child_by_field_name('name'))}' declared without initializer (check definite assignment)")


# --------------------------------------------------------------------------- 5. checked exceptions
def check_exceptions(path, root):
    for call in iter_nodes(root):
        if call.type != "method_invocation":
            continue
        name = text(call.child_by_field_name("name"))
        if name not in CHECKED_CALLS:
            continue
        obj = call.child_by_field_name("object")
        if name == "list" and (obj is None or text(obj) != "Files"):
            continue
        if name == "walk" and (obj is None or text(obj) != "Files"):
            continue
        if name == "invoke" and obj is not None and text(obj) in ("handler", "listener"):
            continue
        if name == "sleep" and obj is not None and text(obj) != "Thread":
            continue
        if name == "newInstance" and obj is not None and not text(obj).startswith(("constructor", "ctor")):
            continue
        # inside a try with catch, or a method that declares throws?
        n = call.parent
        covered = False
        while n is not None:
            if n.type in ("try_statement", "try_with_resources_statement") and any(c.type == "catch_clause" for c in n.children):
                covered = True
                break
            if n.type in ("method_declaration", "constructor_declaration"):
                if any(c.type == "throws" for c in n.children):
                    covered = True
                break
            if n.type == "lambda_expression":
                break  # lambda: depends on functional interface; report for manual review
            n = n.parent
        if not covered:
            warn(path, call, f"call to {name}() may throw {CHECKED_CALLS[name]} without try/catch or throws")


def main(argv):
    paths = [pathlib.Path(p) for p in argv] or [pathlib.Path("src/main/java"), pathlib.Path("src/test/java")]
    files = []
    for p in paths:
        files.extend(sorted(p.rglob("*.java")) if p.is_dir() else [p])
    for f in files:
        root = PARSER.parse(f.read_bytes()).root_node
        rel = str(f)
        check_captures(rel, root)
        check_unreachable(rel, root)
        check_missing_return(rel, root)
        check_uninitialised(rel, root)
        check_exceptions(rel, root)
    for line in findings:
        print(line)
    print(f"scanned {len(files)} files, {len(findings)} findings")


if __name__ == "__main__":
    main(sys.argv[1:])
