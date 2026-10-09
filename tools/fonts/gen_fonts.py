"""Writes Shard's Inter font definitions: one per weight, size and raster density.

    python tools/fonts/gen_fonts.py

Minecraft fixes a TTF provider's size and oversample per definition. `Fonts` picks the
definition whose oversample (pixels per design unit) is closest above the real on-screen
density, so glyphs are rasterised at, or slightly above, the size they are shown at.
Density 2 keeps the 0.3.0 file names (ui-<weight>-<size>.json); others add -x<density>.
"""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/shard/font"
WEIGHTS = ("regular", "medium", "semibold")
SIZES = (10, 11, 12, 13, 14, 15, 16, 18, 20, 24)
DENSITIES = (1, 2, 3, 4, 6)


def definition(weight, size, density):
    return {"providers": [
        {"type": "ttf", "file": f"shard:inter-{weight}.ttf", "size": float(size), "oversample": float(density)},
        {"type": "reference", "id": "minecraft:default"},
    ]}


for weight in WEIGHTS:
    for size in SIZES:
        for density in DENSITIES:
            suffix = "" if density == 2 else f"-x{density}"
            path = OUT / f"ui-{weight}-{size}{suffix}.json"
            path.write_text(json.dumps(definition(weight, size, density), indent=2) + "\n", encoding="utf-8")
print(len(WEIGHTS) * len(SIZES) * len(DENSITIES), "definitions")
