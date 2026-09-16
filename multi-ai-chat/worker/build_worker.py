"""Bakes the frontend into worker_src.js and writes a single worker.js.

The Cloudflare dashboard editor takes one file, so index.html, styles.css,
app.js and models.json are embedded as JSON string literals. The files under
static/ stay the single source of truth - rebuild after editing them:

    python3 worker/build_worker.py
"""

from __future__ import annotations

import json
import pathlib
import sys

HERE = pathlib.Path(__file__).parent
ROOT = HERE.parent
SRC = HERE / "worker_src.js"
OUT = HERE / "worker.js"

ASSETS = {
    "index.html": ROOT / "static" / "index.html",
    "styles.css": ROOT / "static" / "styles.css",
    "app.js": ROOT / "static" / "app.js",
}
MODELS = ROOT / "models.json"


def main() -> int:
    src = SRC.read_text(encoding="utf-8")

    bundle = {name: path.read_text(encoding="utf-8") for name, path in ASSETS.items()}
    models = json.loads(MODELS.read_text(encoding="utf-8"))

    # JSON is a subset of JS object syntax, so json.dumps gives us safe literals.
    # </script> can never appear inside a JS string without escaping the slash.
    assets_js = "const ASSETS = " + json.dumps(bundle, ensure_ascii=False) + ";"
    assets_js = assets_js.replace("</", "<\\/")
    models_js = "const MODELS = " + json.dumps(models, ensure_ascii=False) + ";"

    for marker, replacement in (("// __ASSETS__", assets_js), ("// __MODELS__", models_js)):
        if marker not in src:
            print(f"marker {marker} missing from worker_src.js", file=sys.stderr)
            return 1
        src = src.replace(marker, replacement, 1)

    OUT.write_text(src, encoding="utf-8")

    size = OUT.stat().st_size
    print(f"worker.js written: {size / 1024:.1f} KB")
    for name, path in ASSETS.items():
        print(f"  embedded {name}: {path.stat().st_size / 1024:.1f} KB")
    print(f"  embedded models.json: {len(models.get('featured', []))} featured, "
          f"{sum(len(c['models']) for c in models.get('categories', []))} catalogue models")

    if size > 900_000:
        print("WARNING: approaching the 1 MB Worker limit", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
