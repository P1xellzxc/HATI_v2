# Makes web/tests/qr-fixtures.json with segno, an independent QR encoder (dev only, never shipped):
#   python3 -m venv /tmp/q && /tmp/q/bin/pip install segno && /tmp/q/bin/python -I web/tests/qr-fixtures.py web/tests/qr-fixtures.json
import json, sys, segno
from segno import encoder as _enc
# segno 1.6.6 adds a whole 0x00 byte after the terminator when the bit stream already ends on a byte boundary
# (write_padding_bits extends by 8 - len % 8 even when that is 8). ISO/IEC 18004 7.4.10 pads only up to the
# boundary, which is what the app (and most encoders) do. Both decode the same; for module-exact fixtures,
# segno gets the standard rule here. Everything else (blocks, Reed-Solomon, placement, masks, penalty) is segno's own.
_orig_pad = _enc.write_padding_bits
_enc.write_padding_bits = lambda buff, version, length: _orig_pad(buff, version, length) if length % 8 else None
base = "https://p1xellzxc.github.io/HATI_v2/app/#join="
alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
def filler(n, seed):
    out, x = [], seed
    for _ in range(n):
        x = (x * 1103515245 + 12345) % 2147483648
        out.append(alpha[(x >> 16) % 64])
    return "".join(out)
cases = [
    {"text": base + "abc", "version": 3, "mask": 2},
    {"text": base + filler(200, 7), "version": 10, "mask": 5},
    {"text": base + filler(800, 11), "version": 20, "mask": 7},
    {"text": base + filler(640, 3)},
    {"text": "HATI² invite"},
] + [{"text": base + filler(n, n)} for n in (20, 90, 150, 330, 500, 760)]
out = []
for c in cases:
    kw = dict(error="l", mode="byte", boost_error=False, micro=False)
    if "version" in c: kw.update(version=c["version"], mask=c["mask"])
    q = segno.make(c["text"].encode("utf-8"), **kw)
    m = ["".join("1" if b & 1 else "0" for b in row) for row in q.matrix]
    assert len(m) == q.version * 4 + 17, (len(m), q.version)
    out.append({"text": c["text"], "version": q.version, "mask": q.mask, "forced": "version" in c, "matrix": m})
    print(q.version, q.mask, len(c["text"]), file=sys.stderr)
json.dump({"made_with": "segno " + segno.__version__ + " (dev only; error L, byte mode)", "cases": out}, open(sys.argv[1], "w"), indent=0)
