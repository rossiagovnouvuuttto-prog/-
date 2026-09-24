"""Assembles the Chrome extension from the shared sources in static/.

static/app.js and static/styles.css are copied verbatim, so the extension and
the hosted site stay the same app. Only index.html is adjusted: it loads
static/api.js (the in-page backend) and gains a key field in Settings.

    python3 extension/build_extension.py
"""

from __future__ import annotations

import pathlib
import shutil
import struct
import sys
import zlib

HERE = pathlib.Path(__file__).parent
ROOT = HERE.parent
SRC_STATIC = ROOT / "static"
OUT_STATIC = HERE / "static"

TOKEN_FIELD = """      <div class="field">
        <label for="ollamaKey">Ключ Ollama</label>
        <input type="password" id="ollamaKey" placeholder="Вставьте ключ" autocomplete="off" spellcheck="false">
        <p class="hint">Хранится только в этом браузере и отправляется напрямую в Ollama.
          <a href="https://ollama.com/settings/keys" target="_blank" rel="noopener">Получить ключ</a>
        </p>
      </div>

"""

SETTINGS_ANCHOR = """      <div class="field">
        <label for="systemPrompt">System Prompt</label>"""

SCRIPT_ANCHOR = '<script src="/static/app.js"></script>'
SCRIPT_REPLACEMENT = '<script src="/static/api.js"></script>\n<script src="/static/app.js"></script>'


# --------------------------------------------------------------------------- #
# A tiny PNG writer, so the icons need no imaging library.
# --------------------------------------------------------------------------- #
def write_png(path: pathlib.Path, size: int) -> None:
    ss = 4  # supersample factor, for smooth edges
    big = size * ss
    cx = cy = big / 2 - 0.5
    r = big / 2 - ss  # leave a hair of padding

    RED = (255, 77, 77)
    WHITE = (255, 255, 255)
    INK = (10, 20, 40)

    band = r * 0.16          # dark divider through the middle
    button_r = r * 0.30
    ring = r * 0.09

    acc = [[[0, 0, 0, 0] for _ in range(size)] for _ in range(size)]

    for py in range(big):
        for px in range(big):
            dx, dy = px - cx, py - cy
            dist = (dx * dx + dy * dy) ** 0.5
            if dist > r:
                continue
            if dist <= button_r:
                colour = WHITE if dist <= button_r - ring else INK
            elif abs(dy) <= band / 2:
                colour = INK
            elif dist > r - ss * 1.2:
                colour = INK          # outline
            else:
                colour = RED if dy < 0 else WHITE

            cell = acc[py // ss][px // ss]
            cell[0] += colour[0]
            cell[1] += colour[1]
            cell[2] += colour[2]
            cell[3] += 255

    n = ss * ss
    rows = b""
    for row in acc:
        rows += b"\x00"
        for cell in row:
            a = cell[3] // n
            if a == 0:
                rows += b"\x00\x00\x00\x00"
            else:
                # Average the colour over the COVERED subpixels only, so a
                # partly covered edge pixel keeps its hue and just goes
                # translucent. cell[3] is the summed alpha (255 per covered
                # subpixel), which is exactly that weight.
                rows += struct.pack(
                    "BBBB",
                    min(255, cell[0] * 255 // cell[3]),
                    min(255, cell[1] * 255 // cell[3]),
                    min(255, cell[2] * 255 // cell[3]),
                    a,
                )

    def chunk(kind: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(rows, 9))
           + chunk(b"IEND", b""))
    path.write_bytes(png)


def main() -> int:
    OUT_STATIC.mkdir(parents=True, exist_ok=True)
    (HERE / "icons").mkdir(exist_ok=True)

    # 1. shared assets, copied unchanged
    for name in ("styles.css", "app.js"):
        shutil.copyfile(SRC_STATIC / name, OUT_STATIC / name)
    shutil.copyfile(ROOT / "models.json", HERE / "models.json")

    # 2. index.html, with api.js and the token field added
    html = (SRC_STATIC / "index.html").read_text(encoding="utf-8")

    if SCRIPT_ANCHOR not in html:
        print("could not find the app.js script tag in index.html", file=sys.stderr)
        return 1
    html = html.replace(SCRIPT_ANCHOR, SCRIPT_REPLACEMENT, 1)

    if SETTINGS_ANCHOR not in html:
        print("could not find the System Prompt field in index.html", file=sys.stderr)
        return 1
    html = html.replace(SETTINGS_ANCHOR, TOKEN_FIELD + SETTINGS_ANCHOR, 1)

    (HERE / "index.html").write_text(html, encoding="utf-8")

    # 3. icons
    for size in (16, 32, 48, 128):
        write_png(HERE / "icons" / f"icon{size}.png", size)

    print("extension built:")
    for path in sorted(HERE.rglob("*")):
        if path.is_file() and "node_modules" not in path.parts:
            print(f"  {path.relative_to(HERE)}  ({path.stat().st_size / 1024:.1f} KB)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
