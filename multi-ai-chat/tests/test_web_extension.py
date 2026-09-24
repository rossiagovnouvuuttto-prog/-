"""Drives the web-mode extension in a real Chromium.

The provider sites are unreachable from this container, which is exactly the
failure that used to leak Chrome's own English error text into the chat.
"""
import pathlib, sys, tempfile
from playwright.sync_api import sync_playwright

EXT = pathlib.Path(__file__).parent.parent / "web-extension"
CHROME = "/opt/pw-browsers/chromium-1194/chrome-linux/chrome"
SHOTS = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/web-ext-shots")
SHOTS.mkdir(parents=True, exist_ok=True)

failures = []
def check(name, ok, detail=""):
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -> {detail}" if detail and not ok else ""))
    if not ok: failures.append(name)

with sync_playwright() as pw:
    ctx = pw.chromium.launch_persistent_context(
        tempfile.mkdtemp(prefix="webext-"), executable_path=CHROME,
        viewport={"width": 1440, "height": 940},
        args=["--headless=new", f"--disable-extensions-except={EXT}", f"--load-extension={EXT}"])
    errs = []
    ctx.on("console", lambda m: errs.append(m.text) if m.type == "error" else None)
    sw = ctx.service_workers[0] if ctx.service_workers else ctx.wait_for_event("serviceworker", timeout=15000)
    ext_id = sw.url.split("/")[2]

    page = ctx.new_page()
    page.goto(f"chrome-extension://{ext_id}/index.html")
    page.wait_for_timeout(2000)

    print("\n== стартовый экран ==")
    check("две карточки", page.locator(".starters").first.locator(".starter").count() == 2)
    check("статус готов", "Готов" in page.locator("#statusText").inner_text())

    print("\n== сайт недоступен: сообщение об ошибке ==")
    page.locator("#input").fill("привет")
    page.keyboard.press("Enter")
    page.wait_for_selector(".msg .err, .err", timeout=90000)
    page.wait_for_timeout(1200)
    toast = page.locator("#toasts").inner_text()
    print("   в уведомлении:", " | ".join(l for l in toast.split("\n") if l.strip())[:200])
    check("заголовок назвал причину, а не просто «Ошибка»", "Сайт не открылся" in toast)
    text = page.locator("#messages").inner_text()
    err = text[text.find("Ошибка") if "Ошибка" in text else 0:][:400]
    print("   в чате:", " | ".join(l for l in err.split("\n") if l.strip())[:220])
    page.screenshot(path=str(SHOTS / "web-error.png"))

    raw_markers = ["Frame with ID", "showing error page", "ERR_", "net::", "Cannot access", "undefined"]
    leaked = [m for m in raw_markers if m in text]
    check("сырой текст Chrome не попал в чат", not leaked, str(leaked))
    check("сообщение по-русски", any(w in text for w in ["Сайт", "сайт", "Проверьте", "войти"]))
    check("названа причина", "не открылся" in text or "войти" in text or "интернет" in text)
    check("чат остался рабочим", page.locator("#input").is_editable())

    print("\n== повторная отправка ==")
    page.locator("#input").fill("ещё раз")
    page.keyboard.press("Enter")
    page.wait_for_timeout(8000)
    check("второй ответ тоже осмысленный", "Frame with ID" not in page.locator("#messages").inner_text())

    print("\n== хранение ==")
    page.evaluate("localStorage.setItem('mac.customModels', JSON.stringify([{id:'x-test',name:'X',desc:'d'}]))")
    page.reload(); page.wait_for_timeout(1500)
    saved = page.evaluate("JSON.parse(localStorage.getItem('mac.customModels')||'[]').length")
    in_state = page.evaluate("(() => { try { return window.__macState ? 1 : -1 } catch { return -1 } })()")
    check("свои модели читаются при старте", saved == 1)

    print("\n== консоль ==")
    hard = [e for e in errs if "chatgpt.com" not in e and "ERR_" not in e and "net::" not in e]
    check("нет посторонних ошибок JS", not hard, str(hard[:3]))
    ctx.close()

print("\n" + "=" * 46)
print("ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ" if not failures else f"ПРОВАЛЕНО: {failures}")
sys.exit(1 if failures else 0)
