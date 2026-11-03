"""Export the Sanzo Wada palettes from HatiPalette.kt as JSON for the web app."""
import json, re, sys
from pathlib import Path

KT = Path(__file__).resolve().parents[2] / "app/src/main/java/com/hativ2/ui/theme/HatiPalette.kt"

def palettes():
    s = KT.read_text()
    out = []
    for name, body in re.findall(r"val (\w+) = HatiPalette\((.*?)\n    \)", s, re.S):
        v = dict(re.findall(r"(\w+) = Color\(0xFF([0-9A-F]{6})\)", body))
        sw = re.findall(r"0xFF([0-9A-F]{6})", re.search(r"swatches = listOf\((.*?)\)\),", body, re.S).group(1) + ")")
        n = re.search(r"wadaNumber = (\w+)", body).group(1)
        out.append(dict(id=re.search(r'id = "(\w+)"', body).group(1), name=name, n=None if n == "null" else int(n),
                        sw=["#" + x for x in sw], ink="#" + v["ink"], paper="#" + v["paper"], acc="#" + v["accent"],
                        tert="#" + v["tertiary"], hero="#" + v["hero"], food="#" + v["categoryFood"], muted="#" + v["muted"],
                        accD="#" + v["darkAccent"], heroD="#" + v["darkHero"]))
    if not out:
        sys.exit("No palettes found in " + str(KT))
    return out

if __name__ == "__main__":
    print(json.dumps(palettes(), separators=(",", ":")))
