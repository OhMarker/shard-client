"""Builds Shard's icon atlases from Lucide (ISC licence) with headless Microsoft Edge.

    python tools/icons/build_icons.py

Check names first: open atlas.html?s=64 from a local server in a browser and read #report
(it lists unknown Lucide names). Serves this folder on localhost, lets atlas.html draw every icon in icons.txt as white strokes
on black at 32, 64 and 128 px per cell, screenshots each page with Edge, turns luminance into
alpha (white RGB, so the game tints it) and writes
src/main/resources/assets/shard/textures/gui/icons_<size>.png plus icons.json (name -> cell).
"""
import functools
import http.server
import json
import os

import subprocess
import tempfile
import threading
import time
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
OUT = HERE.parents[1] / "src/main/resources/assets/shard/textures/gui"
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
COLS = 16
SIZES = (32, 64, 128)
PORT = 8766


def names():
    out = []
    for line in (HERE / "icons.txt").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            out.append(line.split()[0])
    return out


def edge(args, profile):
    cmd = [EDGE, "--headless", "--disable-gpu", "--hide-scrollbars", "--no-first-run",
           "--force-device-scale-factor=1", "--virtual-time-budget=8000",
           f"--user-data-dir={profile}"] + args
    return subprocess.run(cmd, capture_output=True, text=True, timeout=120)


def main():
    handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=str(HERE))
    server = http.server.ThreadingHTTPServer(("127.0.0.1", PORT), handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    icons = names()
    rows = -(-len(icons) // COLS)
    OUT.mkdir(parents=True, exist_ok=True)
    try:
        with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
            for size in SIZES:
                shot = os.path.join(tmp, f"atlas-{size}.png")
                w, h = COLS * size, rows * size
                edge([f"--window-size={w},{h}", f"--screenshot={shot}",
                      f"http://127.0.0.1:{PORT}/atlas.html?s={size}"], os.path.join(tmp, f"p{size}"))
                # msedge.exe hands off to a browser process and returns early: wait for the file.
                for _ in range(120):
                    if os.path.exists(shot) and os.path.getsize(shot) > 0:
                        break
                    time.sleep(0.5)
                time.sleep(0.5)
                src = Image.open(shot).convert("L").crop((0, 0, w, h))
                atlas = Image.new("RGBA", (w, h), (255, 255, 255, 0))
                atlas.putalpha(src)
                atlas.save(OUT / f"icons_{size}.png", optimize=True)
                (OUT / f"icons_{size}.png.mcmeta").write_text('{"texture": {"blur": true, "clamp": true}}\n')
                print(f"icons_{size}.png {w}x{h}")
    finally:
        server.shutdown()
    index = {"columns": COLS, "rows": rows, "sizes": list(SIZES), "icons": {n: i for i, n in enumerate(icons)}}
    (OUT / "icons.json").write_text(json.dumps(index, indent=1) + "\n")
    print(f"{len(icons)} icons")


if __name__ == "__main__":
    main()
