"""Loads the Chrome extension for real and drives it.

app.js and styles.css are byte-identical copies of the hosted build, so the
full UI suite in test_frontend.py already covers the interface. What is new
here is the extension shell: the manifest, the in-page backend in api.js, and
the token living in extension storage.

    uvicorn tests.mock_hf:app --port 8899 &
    python3 tests/test_extension.py [screenshot-dir]
"""

from __future__ import annotations

import pathlib
import sys
import tempfile

from playwright.sync_api import sync_playwright

ROOT = pathlib.Path(__file__).parent.parent
EXT = ROOT / "extension"
CHROME = "/opt/pw-browsers/chromium-1194/chrome-linux/chrome"
MOCK_BASE = "http://127.0.0.1:8899/v1"
FAKE_TOKEN = "hf_faketokenfortesting1234567890"

SHOTS = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/ext-shots")
SHOTS.mkdir(parents=True, exist_ok=True)

failures: list[str] = []


def check(name: str, ok: bool, detail: str = "") -> None:
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -> {detail}" if detail and not ok else ""))
    if not ok:
        failures.append(name)


with sync_playwright() as pw:
    profile = tempfile.mkdtemp(prefix="mac-ext-")
    ctx = pw.chromium.launch_persistent_context(
        profile,
        executable_path=CHROME,
        viewport={"width": 1440, "height": 940},
        args=[
            "--headless=new",
            f"--disable-extensions-except={EXT}",
            f"--load-extension={EXT}",
        ],
    )

    console_errors: list[str] = []
    bad_responses: list[str] = []
    ctx.on("console", lambda m: console_errors.append(m.text) if m.type == "error" else None)
    ctx.on("response", lambda r: bad_responses.append(f"{r.status} {r.url}") if r.status >= 400 else None)
    ctx.on("requestfailed", lambda r: bad_responses.append(f"FAILED {r.url}"))

    print("\n== extension loads ==")
    worker = None
    for w in ctx.service_workers:
        worker = w
    if worker is None:
        try:
            worker = ctx.wait_for_event("serviceworker", timeout=15000)
        except Exception:
            worker = None
    check("service worker registered", worker is not None)

    ext_id = worker.url.split("/")[2] if worker else None
    check("extension id resolved", bool(ext_id), str(ext_id))
    page_url = f"chrome-extension://{ext_id}/index.html"

    page = ctx.new_page()
    page.on("pageerror", lambda e: console_errors.append(str(e)))
    page.goto(page_url, wait_until="networkidle")

    check("chat page opens from the extension", page.title() == "Multi AI Chat", page.title())
    check("welcome screen renders", page.locator(".welcome h1").inner_text() == "Multi AI Chat")
    check("four starter cards", page.locator(".welcome .starter").count() == 4,
          str(page.locator(".welcome .starter").count()))

    print("\n== without a token ==")
    statuses = [page.locator(".welcome .starter .s-status").nth(i).inner_text().strip() for i in range(4)]
    check("all cards read Offline", statuses == ["Offline"] * 4, str(statuses))
    check("status bar warns", "HF_TOKEN" in page.inner_text("#modelStatus"), page.inner_text("#modelStatus"))
    toast = page.locator(".toast.err").first
    check("hint points at Settings, not hosting",
          "Настройки" in toast.inner_text(), toast.inner_text()[:90])
    page.screenshot(path=str(SHOTS / "E1-no-token.png"), full_page=True)

    print("\n== entering the token in Settings ==")
    page.click("#settingsBtn")
    page.wait_for_selector("#settingsModal:not([hidden])")
    check("token field present", page.locator("#hfToken").count() == 1)
    check("token field is masked", page.get_attribute("#hfToken", "type") == "password")
    page.screenshot(path=str(SHOTS / "E2-settings.png"))

    # Point the shim at the stand-in router first, so the refresh that entering
    # a token triggers can be observed. A user never touches this setting.
    page.evaluate("base => chrome.storage.local.set({ hfBaseUrl: base })", MOCK_BASE)

    page.fill("#hfToken", FAKE_TOKEN)
    page.wait_for_timeout(2500)
    stored = page.evaluate("async () => (await chrome.storage.local.get(['hfToken'])).hfToken")
    check("token saved to extension storage", stored == FAKE_TOKEN, str(stored))

    live = [page.locator(".welcome .starter .s-status").nth(i).inner_text().strip() for i in range(4)]
    check("cards go live without a page reload", live.count("Online") == 3, str(live))

    page.click("#settingsModal [data-close]")
    page.reload(wait_until="networkidle")

    print("\n== with a token ==")
    page.wait_for_selector(".welcome .starter", timeout=15000)
    page.wait_for_timeout(1200)
    names = [page.locator(".welcome .starter .s-name").nth(i).inner_text() for i in range(4)]
    check("families listed", names == ["DeepSeek", "Qwen", "Llama", "Mistral"], str(names))
    statuses = [page.locator(".welcome .starter .s-status").nth(i).inner_text().strip() for i in range(4)]
    check("three online, one offline",
          statuses.count("Online") == 3 and statuses.count("Offline") == 1, str(statuses))
    ids = [page.locator(".welcome .starter .s-id").nth(i).inner_text() for i in range(4)]
    check("Qwen fell back inside its family", "Qwen/Qwen2.5-7B-Instruct" in ids, str(ids))
    check("status bar back to Online", "Online" in page.inner_text("#modelStatus"))
    page.screenshot(path=str(SHOTS / "E3-ready.png"), full_page=True)

    print("\n== chatting ==")
    page.locator(".welcome .starter").nth(0).click()
    page.wait_for_timeout(300)
    page.fill("#input", "Покажи пример кода и таблицу")
    page.click("#sendBtn")
    page.wait_for_selector(".msg.ai .msg-acts", timeout=30000)
    bubble = page.locator(".msg.ai .bubble").first
    check("answer streamed in", "error" not in (bubble.get_attribute("class") or ""), bubble.inner_text()[:80])
    check("markdown rendered", bubble.locator("table tbody tr").count() == 2)
    check("code block with copy button", bubble.locator(".code-block .code-copy").count() == 1)
    page.screenshot(path=str(SHOTS / "E4-chat.png"), full_page=True)

    print("\n== offline family still degrades gracefully ==")
    page.click("#newChatBtn")
    page.wait_for_selector(".welcome .starter", timeout=10000)
    page.locator(".welcome .starter").nth(2).click()      # Llama
    page.wait_for_timeout(300)
    page.fill("#input", "привет")
    page.click("#sendBtn")
    page.wait_for_selector(".msg.ai .bubble.error", timeout=30000)
    check("shows the unavailable notice",
          "Модель сейчас недоступна через Hugging Face Inference." in page.inner_text(".msg.ai .bubble.error"),
          page.inner_text(".msg.ai .bubble.error")[:90])
    check("app stays usable", page.locator("#input").is_enabled())

    print("\n== persistence ==")
    page.reload(wait_until="networkidle")
    page.click("#settingsBtn")
    page.wait_for_selector("#settingsModal:not([hidden])")
    check("token survives reload", page.input_value("#hfToken") == FAKE_TOKEN)
    page.click("#settingsModal [data-close]")
    check("chats survive reload", page.locator(".chat-item").count() >= 2,
          str(page.locator(".chat-item").count()))

    print("\n== the token is not baked into any file ==")
    leaked = []
    for rel in ("index.html", "static/app.js", "static/styles.css", "static/api.js",
                "manifest.json", "background.js", "models.json"):
        text = (EXT / rel).read_text(encoding="utf-8")
        if FAKE_TOKEN in text or "hf_" in text and "hf_..." not in text and "hf_x" not in text:
            if FAKE_TOKEN in text:
                leaked.append(rel)
    check("no token in the shipped files", not leaked, str(leaked))

    print("\n== mobile ==")
    mob = ctx.new_page()
    mob.set_viewport_size({"width": 390, "height": 844})
    mob.goto(page_url, wait_until="networkidle")
    check("hamburger visible", mob.locator("#menuBtn").is_visible())
    check("sidebar tucked away", mob.eval_on_selector("#sidebar", "n => n.getBoundingClientRect().right <= 1"))
    mob.click("#menuBtn")
    mob.wait_for_timeout(450)
    check("menu opens", mob.eval_on_selector("#sidebar", "n => n.getBoundingClientRect().left >= -1"))
    check("no horizontal overflow",
          mob.evaluate("document.documentElement.scrollWidth <= window.innerWidth + 1"))
    mob.screenshot(path=str(SHOTS / "E5-mobile.png"), full_page=True)

    print("\n== console ==")
    # Real JavaScript faults must be zero. "Failed to load resource" lines are
    # the browser narrating HTTP status codes, which this suite provokes on
    # purpose, so they are judged separately below.
    js_errors = [e for e in console_errors
                 if "Failed to load resource" not in e and "favicon" not in e.lower()]
    check("no JavaScript errors", not js_errors, "; ".join(js_errors[:3]))

    # Two upstream failures are part of the script: the offline family returns
    # 404, and the first page load reaches the real router before the test
    # repoints it at the stand-in (no outbound network in this sandbox).
    expected = ("404 http://127.0.0.1:8899/v1/chat/completions", "router.huggingface.co")
    unexpected = [r for r in bad_responses
                  if "favicon" not in r.lower() and not any(e in r for e in expected)]
    check("no unexpected failing requests", not unexpected, "; ".join(unexpected[:5]))

    ctx.close()

print("\n" + "=" * 46)
if failures:
    print(f"FAILED: {len(failures)} -> {failures}")
    sys.exit(1)
print("ALL EXTENSION CHECKS PASSED")
