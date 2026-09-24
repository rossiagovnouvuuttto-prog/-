"""Rebuilds the picture marks for the anime / Minecraft / Roblox / GTA themes.

The PNGs in static/themes/ are the source: <theme>.png is the square logo mark,
<theme>-wide.png the strip shown as a sample in Settings. Two shapes, because a
square cropped into a short strip cuts the lettering off the Minecraft and GTA
logos. They are inlined into styles.css as data URIs so all three builds — site,
Cloudflare Worker and extension — pick them up without any extra asset plumbing:
the stylesheet is already copied verbatim into each.

    python3 tools/make_theme_marks.py
"""

from __future__ import annotations

import base64
import pathlib

ROOT = pathlib.Path(__file__).parent.parent
CSS = ROOT / "static" / "styles.css"
THEMES = ROOT / "static" / "themes"

START = "/* ══════════════ Картинки тем (генерируется) ══════════════ */"
END = "/* ══════════════ Конец картинок тем ══════════════ */"


def uri(png: pathlib.Path) -> str:
    return "data:image/png;base64," + base64.b64encode(png.read_bytes()).decode()


def block() -> str:
    out = [START,
           "/* Собрано tools/make_theme_marks.py из static/themes/*.png —",
           "   правьте картинки и пересоберите, а не эти строки. */"]
    for theme in ("anime", "minecraft", "roblox", "gta"):
        mark = uri(THEMES / f"{theme}.png")
        wide = uri(THEMES / f"{theme}-wide.png")
        out += [
            f":root[data-theme={theme}] .brand-mark,",
            f":root[data-theme={theme}] .welcome-logo{{",
            f'  background:url("{mark}") center/cover no-repeat;',
            "}",
            f":root[data-theme={theme}] .brand-mark::after,",
            f":root[data-theme={theme}] .welcome-logo::after{{content:none;display:none}}",
            f'.themes button[data-t={theme}] .t-swatch{{background:url("{wide}") center/cover no-repeat}}',
        ]
    out.append(END)
    return "\n".join(out) + "\n"


def main() -> int:
    css = CSS.read_text(encoding="utf-8")
    fresh = block()
    if START in css and END in css:
        head, rest = css.split(START, 1)
        css = head + fresh + rest.split(END, 1)[1].lstrip("\n")
    else:
        css = css.rstrip("\n") + "\n\n" + fresh
    CSS.write_text(css, encoding="utf-8")
    print(f"styles.css: {CSS.stat().st_size / 1024:.1f} KB")
    for theme in ("anime", "minecraft", "roblox", "gta"):
        square = (THEMES / f"{theme}.png").stat().st_size / 1024
        strip = (THEMES / f"{theme}-wide.png").stat().st_size / 1024
        print(f"  {theme:10s} {square:5.1f} KB + {strip:5.1f} KB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
