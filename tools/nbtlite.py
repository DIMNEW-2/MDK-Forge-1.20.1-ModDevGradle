"""Minimal pure-python NBT writer (big-endian, gzip) sufficient for structure templates.

Values are mapped as follows:
    Byte(v) / Short(v) / Int(v) / Long(v) / Float(v) / Double(v)  -> explicit numeric tags
    str                                                         -> TAG_String
    dict                                                        -> TAG_Compound (insertion order kept)
    list                                                        -> TAG_List (element type taken from the first element,
                                                                   empty list -> TAG_List of TAG_End)
    int                                                         -> TAG_Int, float -> TAG_Double
"""
import gzip
import struct


class _Num:
    __slots__ = ("value",)

    def __init__(self, value):
        self.value = value


class Byte(_Num):
    TAG = 1
    FMT = ">b"


class Short(_Num):
    TAG = 2
    FMT = ">h"


class Int(_Num):
    TAG = 3
    FMT = ">i"


class Long(_Num):
    TAG = 4
    FMT = ">q"


class Float(_Num):
    TAG = 5
    FMT = ">f"


class Double(_Num):
    TAG = 6
    FMT = ">d"


TAG_END, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 8, 9, 10


def _tag_type(value):
    if isinstance(value, _Num):
        return value.TAG
    if isinstance(value, bool):
        return Byte.TAG
    if isinstance(value, int):
        return Int.TAG
    if isinstance(value, float):
        return Double.TAG
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, dict):
        return TAG_COMPOUND
    if isinstance(value, (list, tuple)):
        return TAG_LIST
    raise TypeError("unsupported NBT value: %r" % (value,))


def _write_string(out, s):
    data = s.encode("utf-8")
    out.append(struct.pack(">H", len(data)))
    out.append(data)


def _write_payload(out, value):
    kind = _tag_type(value)
    if kind == TAG_STRING:
        _write_string(out, value)
    elif kind == TAG_COMPOUND:
        for key, item in value.items():
            out.append(struct.pack(">b", _tag_type(item)))
            _write_string(out, key)
            _write_payload(out, item)
        out.append(b"\x00")
    elif kind == TAG_LIST:
        items = list(value)
        elem = _tag_type(items[0]) if items else TAG_END
        for item in items:
            if _tag_type(item) != elem:
                raise TypeError("heterogeneous NBT list")
        out.append(struct.pack(">bi", elem, len(items)))
        for item in items:
            _write_payload(out, item)
    elif isinstance(value, _Num):
        out.append(struct.pack(value.FMT, value.value))
    elif isinstance(value, bool):
        out.append(struct.pack(">b", 1 if value else 0))
    elif isinstance(value, int):
        out.append(struct.pack(">i", value))
    else:
        out.append(struct.pack(">d", value))


def dumps(root, name=""):
    """Serialises a dict as an (unnamed by default) root compound; returns raw bytes."""
    out = [struct.pack(">b", TAG_COMPOUND)]
    _write_string(out, name)
    _write_payload(out, root)
    return b"".join(out)


def write_gzip(path, root):
    with gzip.open(path, "wb", compresslevel=9) as fh:
        fh.write(dumps(root))


# ----------------------------------------------------------------------------- tiny reader (for self-checks)
def loads(data):
    pos = [0]

    def read(fmt):
        size = struct.calcsize(fmt)
        value = struct.unpack(fmt, data[pos[0]:pos[0] + size])[0]
        pos[0] += size
        return value

    def read_string():
        length = read(">H")
        s = data[pos[0]:pos[0] + length].decode("utf-8")
        pos[0] += length
        return s

    def read_payload(kind):
        if kind == 1:
            return read(">b")
        if kind == 2:
            return read(">h")
        if kind == 3:
            return read(">i")
        if kind == 4:
            return read(">q")
        if kind == 5:
            return read(">f")
        if kind == 6:
            return read(">d")
        if kind == 7:
            n = read(">i")
            return [read(">b") for _ in range(n)]
        if kind == 8:
            return read_string()
        if kind == 9:
            elem = read(">b")
            n = read(">i")
            return [read_payload(elem) for _ in range(n)]
        if kind == 10:
            result = {}
            while True:
                t = read(">b")
                if t == 0:
                    return result
                key = read_string()
                result[key] = read_payload(t)
        if kind == 11:
            n = read(">i")
            return [read(">i") for _ in range(n)]
        if kind == 12:
            n = read(">i")
            return [read(">q") for _ in range(n)]
        raise ValueError("bad tag %d" % kind)

    kind = read(">b")
    assert kind == TAG_COMPOUND
    read_string()
    return read_payload(kind)


def read_gzip(path):
    with gzip.open(path, "rb") as fh:
        return loads(fh.read())
