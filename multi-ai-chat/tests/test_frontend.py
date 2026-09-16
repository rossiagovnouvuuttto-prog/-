"""Browser-level checks driven through Chromium (Playwright).

Exercises the real UI against the running app + mock Hugging Face router.
"""

from __future__ import annotations

import os
import pathlib
import sys

from playwright.sync_api import expect, sync_playwright

APP = os.environ.get("APP_URL", "http://127.0.0.1:8800")
SHOTS = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/shots")
SHOTS.mkdir(parents=True, exist_ok=True)

failures: list[str] = []


def check(name: str, ok: bool, detail: str = "") -> None:
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -> {detail}" if detail and not ok else ""))
    if not ok:
        failures.append(name)


def pick_model(page, model_id: str) -> None:
    """Adds `model_id` through the picker's custom-model field and selects it."""
    page.click("#pickModelBtn")
    page.fill("#customModel", model_id)
    page.click("#addModelBtn")
    page.wait_for_selector("#modelModal", state="hidden", timeout=5000)


def send(page, text: str) -> None:
    page.fill("#input", text)
    page.click("#sendBtn")


with sync_playwright() as pw:
    browser = pw.chromium.launch(executable_path="/opt/pw-browsers/chromium-1194/chrome-linux/chrome")
    ctx = browser.new_context(viewport={"width": 1440, "height": 950})
    page = ctx.new_page()

    console_errors: list[str] = []
    page.on("console", lambda m: console_errors.append(m.text) if m.type == "error" else None)
    page.on("pageerror", lambda e: console_errors.append(str(e)))

    print("\n== landing ==")
    page.goto(APP, wait_until="networkidle")
    check("title", page.title() == "Multi AI Chat", page.title())
    check("welcome heading", page.inner_text(".welcome h1") == "Multi AI Chat")
    check("welcome subtitle", "Выберите нейросеть" in page.inner_text(".welcome p"))
    check("four quick actions", page.locator(".quick button").count() == 4)
    check("quick labels", "Помочь с Minecraft" in page.inner_text(".quick"))
    check("model name shown", page.inner_text("#modelName").strip() not in ("", "—"),
          page.inner_text("#modelName"))
    check("status online", "Online" in page.inner_text("#modelStatus"))
    check("composer placeholder",
          page.get_attribute("#input", "placeholder") == "Напишите сообщение...")
    page.screenshot(path=str(SHOTS / "01-welcome.png"), full_page=True)

    print("\n== starter cards (the four models) ==")
    cards = page.locator(".welcome .starters .starter")
    check("four starter cards", cards.count() == 4, str(cards.count()))
    names = [cards.nth(i).locator(".s-name").inner_text() for i in range(cards.count())]
    check("DeepSeek card", "DeepSeek" in names, str(names))
    check("Qwen card", "Qwen" in names, str(names))
    check("Llama card", "Llama" in names, str(names))
    check("Mistral card", "Mistral" in names, str(names))

    descs = [cards.nth(i).locator(".s-desc").inner_text() for i in range(4)]
    check("each card has a description", all(d.strip() for d in descs), str(descs))
    icons = [cards.nth(i).locator(".s-icon").inner_text() for i in range(4)]
    check("each card has an icon", all(i.strip() for i in icons), str(icons))

    statuses = [cards.nth(i).locator(".s-status").inner_text().strip() for i in range(4)]
    check("status pill on every card",
          all(t in ("Online", "Offline") for t in statuses), str(statuses))
    check("online and offline both rendered",
          statuses.count("Online") == 3 and statuses.count("Offline") == 1, str(statuses))
    check("cards are type-coloured",
          len({cards.nth(i).get_attribute("data-type") for i in range(4)}) == 4)
    check("bright card text is dark for contrast",
          cards.nth(0).evaluate("n => getComputedStyle(n).color") in
          ("rgb(10, 26, 51)",), cards.nth(0).evaluate("n => getComputedStyle(n).color"))
    page.screenshot(path=str(SHOTS / "01b-starters.png"), full_page=True)

    cards.nth(0).click()
    page.wait_for_timeout(300)
    check("clicking a card switches model",
          "DeepSeek" in page.inner_text("#modelName"), page.inner_text("#modelName"))
    check("chosen card is marked",
          page.locator(".welcome .starter.chosen").count() == 1)

    print("\n== model picker ==")
    page.click("#pickModelBtn")
    page.wait_for_selector("#modelModal:not([hidden])")
    cats = page.inner_text("#pickerCats")
    for name in ("DeepSeek", "Qwen", "Llama", "Mistral", "Gemma", "Microsoft"):
        check(f"category {name}", name in cats)
    check("model cards listed", page.locator(".model-card").count() >= 20,
          str(page.locator(".model-card").count()))
    check("picker shows the starter row",
          page.locator("#pickerStarters .starter").count() == 4,
          str(page.locator("#pickerStarters .starter").count()))
    page.screenshot(path=str(SHOTS / "02-model-picker.png"))

    page.click("#pickerCats button:has-text('DeepSeek')")
    check("category filters", page.locator(".model-card").count() == 4,
          str(page.locator(".model-card").count()))
    page.fill("#modelSearch", "coder")
    check("search filters", page.locator(".model-card").count() == 1,
          str(page.locator(".model-card").count()))
    page.fill("#modelSearch", "")
    page.click("#pickerCats button:has-text('Все модели')")
    page.click(".model-card:has-text('Qwen2.5 72B')")
    page.wait_for_selector("#modelModal", state="hidden")
    check("model switched", "Qwen2.5 72B" in page.inner_text("#modelName"),
          page.inner_text("#modelName"))

    print("\n== custom model by Model ID ==")
    page.click("#pickModelBtn")
    page.fill("#customModel", "not a model")
    page.click("#addModelBtn")
    check("rejects bad Model ID", page.locator(".toast.err").count() >= 1)
    page.fill("#customModel", "mock/ok")
    page.click("#addModelBtn")
    page.wait_for_selector("#modelModal", state="hidden")
    check("custom model selected", "ok" in page.inner_text("#modelName"), page.inner_text("#modelName"))

    print("\n== chat + streaming + markdown ==")
    send(page, "Покажи пример кода и таблицу")
    page.wait_for_selector(".msg.user", timeout=5000)
    check("user message on the right",
          page.eval_on_selector(".msg.user", "n => getComputedStyle(n).flexDirection") == "row-reverse")
    check("typing indicator", page.locator(".typing, .caret").count() >= 1)
    page.wait_for_selector(".btn-stop:not([hidden])", timeout=5000)
    check("stop button visible while generating", page.locator(".btn-stop:visible").count() == 1)

    page.wait_for_selector(".msg.ai .msg-acts", timeout=30000)
    bubble = page.locator(".msg.ai .bubble").first
    check("renders heading", bubble.locator("h1").count() >= 1)
    check("renders bold", bubble.locator("strong").count() >= 1)
    check("renders inline code", bubble.locator("code:not(pre code)").count() >= 1)
    check("renders link", bubble.locator("a[href='https://huggingface.co']").count() == 1)
    check("renders bullet list", bubble.locator("ul li").count() >= 3)
    check("renders nested list", bubble.locator("ul ul li").count() >= 1)
    check("renders ordered list", bubble.locator("ol li").count() >= 2)
    check("renders table", bubble.locator("table tbody tr").count() == 2)
    check("renders blockquote", bubble.locator("blockquote").count() == 1)
    check("renders code block", bubble.locator(".code-block pre code").count() == 1)
    check("code block labelled python", "PYTHON" in bubble.inner_text().upper())
    check("code copy button", bubble.locator(".code-copy:has-text('Копировать код')").count() == 1)
    check("syntax highlighting", bubble.locator(".code-block .tok-kw").count() >= 1)
    check("stop button hidden after finish", page.locator(".btn-stop:visible").count() == 0)

    acts = page.locator(".msg.ai .msg-acts").first
    check("copy action", acts.locator("button:has-text('Копировать')").count() == 1)
    check("regenerate action", acts.locator("button:has-text('Повторить ответ')").count() == 1)
    check("thumbs up/down", acts.locator("button").count() == 4)
    page.screenshot(path=str(SHOTS / "03-chat.png"), full_page=True)

    acts.locator("button").nth(2).click()
    check("thumbs up toggles", page.locator(".msg.ai .msg-acts .act.on").count() == 1)

    print("\n== history ==")
    check("chat saved to history", page.locator(".chat-item").count() == 1)
    check("title from first message",
          "Покажи пример кода" in page.inner_text(".chat-item .chat-title"))
    page.click("#newChatBtn")
    check("new chat resets to welcome", page.locator(".welcome").count() == 1)
    check("history keeps previous chat", page.locator(".chat-item").count() == 2)

    page.locator(".chat-item").nth(1).hover()
    page.locator(".chat-item").nth(1).locator("button.ren").click()
    page.fill("#renameInput", "Проверка Markdown")
    page.click("#renameSave")
    check("rename works", "Проверка Markdown" in page.inner_text("#chatList"))

    page.fill("#searchInput", "Markdown")
    check("search filters history", page.locator(".chat-item").count() == 1)
    page.fill("#searchInput", "zzzznotfound")
    check("search empty state", "Ничего не найдено" in page.inner_text("#chatList"))
    page.fill("#searchInput", "")

    page.click(".chat-item:has-text('Проверка Markdown') .chat-title")
    check("reopening restores messages", page.locator(".msg").count() == 2)

    print("\n== settings ==")
    page.click("#settingsBtn")
    page.wait_for_selector("#settingsModal:not([hidden])")
    check("system prompt field", page.locator("#systemPrompt").count() == 1)
    check("temperature default", page.inner_text("#temperatureOut") == "0.70",
          page.inner_text("#temperatureOut"))
    check("max tokens default", page.inner_text("#maxTokensOut") == "2048")
    check("top p default", page.inner_text("#topPOut") == "0.95")
    page.fill("#systemPrompt", "Отвечай коротко.")
    page.eval_on_selector("#temperature", "n => { n.value = 1.2; n.dispatchEvent(new Event('input')) }")
    check("temperature updates", page.inner_text("#temperatureOut") == "1.20",
          page.inner_text("#temperatureOut"))
    page.screenshot(path=str(SHOTS / "04-settings.png"))
    page.click("#resetSettings")
    check("reset restores default", page.inner_text("#temperatureOut") == "0.70")
    page.click("#settingsModal [data-close]")
    page.wait_for_selector("#settingsModal", state="hidden")

    print("\n== themes ==")
    page.click("#settingsBtn")
    page.wait_for_selector("#settingsModal:not([hidden])")
    picker = page.locator("#themePicker button")
    check("five themes offered", picker.count() == 5, str(picker.count()))
    labels = [picker.nth(i).inner_text().strip() for i in range(picker.count())]
    for want in ("Pokémon", "Аниме", "Minecraft", "Roblox", "GTA SA"):
        check(f"theme {want} listed", any(want in l for l in labels), str(labels))
    check("Pokémon active by default",
          "active" in (picker.nth(0).get_attribute("class") or ""),
          picker.nth(0).get_attribute("class"))
    check("document starts on the pokemon theme",
          page.evaluate("document.documentElement.dataset.theme") == "pokemon")

    before = page.evaluate("getComputedStyle(document.body).backgroundColor")
    page.locator("#themePicker button[data-t=minecraft]").click()
    page.wait_for_timeout(400)
    check("switching sets the theme attribute",
          page.evaluate("document.documentElement.dataset.theme") == "minecraft",
          page.evaluate("document.documentElement.dataset.theme"))
    after = page.evaluate("getComputedStyle(document.body).backgroundColor")
    check("switching repaints the page", before != after, f"{before} -> {after}")
    check("minecraft squares the corners",
          page.evaluate("getComputedStyle(document.querySelector('.composer')).borderRadius").startswith("0"),
          page.evaluate("getComputedStyle(document.querySelector('.composer')).borderRadius"))
    check("active marker moves",
          "active" in (page.locator("#themePicker button[data-t=minecraft]").get_attribute("class") or ""))
    page.screenshot(path=str(SHOTS / "09-theme-minecraft.png"), full_page=True)

    for theme in ("anime", "roblox", "gta"):
        page.locator(f"#themePicker button[data-t={theme}]").click()
        page.wait_for_timeout(250)
        check(f"{theme} applies",
              page.evaluate("document.documentElement.dataset.theme") == theme)
        page.screenshot(path=str(SHOTS / f"09-theme-{theme}.png"), full_page=True)

    page.click("#settingsModal [data-close]")
    page.reload(wait_until="networkidle")
    check("theme survives reload",
          page.evaluate("document.documentElement.dataset.theme") == "gta",
          page.evaluate("document.documentElement.dataset.theme"))

    page.click("#settingsBtn")
    page.wait_for_selector("#settingsModal:not([hidden])")
    page.click("#resetSettings")
    page.wait_for_timeout(300)
    check("reset returns to Pokémon",
          page.evaluate("document.documentElement.dataset.theme") == "pokemon",
          page.evaluate("document.documentElement.dataset.theme"))
    page.click("#settingsModal [data-close]")
    page.wait_for_selector("#settingsModal", state="hidden")

    print("\n== error handling ==")
    page.click("#newChatBtn")
    pick_model(page, "mock/missing")
    send(page, "это должно упасть")
    page.wait_for_selector(".msg.ai .bubble.error", timeout=20000)
    err_text = page.inner_text(".msg.ai .bubble.error")
    check("shows the required unavailable message",
          "Модель сейчас недоступна через Hugging Face Inference." in err_text, err_text)
    check("error toast shown", page.locator(".toast.err").count() >= 1)
    check("status turns red", "Ошибка" in page.inner_text("#modelStatus"))
    check("app still usable", page.locator("#input").is_enabled())
    page.screenshot(path=str(SHOTS / "05-error.png"), full_page=True)

    page.click("#newChatBtn")
    pick_model(page, "mock/unauthorized")
    send(page, "плохой токен")
    page.wait_for_selector(".msg.ai .bubble.error", timeout=20000)
    check("bad token message", "HF_TOKEN" in page.inner_text(".msg.ai .bubble.error"),
          page.inner_text(".msg.ai .bubble.error"))

    print("\n== stop generation ==")
    page.click("#newChatBtn")
    pick_model(page, "mock/slow")
    send(page, "останови меня")
    page.wait_for_selector(".btn-stop:not([hidden])", timeout=8000)
    page.wait_for_timeout(1300)
    page.click("#stopBtn")
    page.wait_for_selector(".msg.ai .msg-acts", timeout=10000)
    check("stop halts generation", page.locator(".btn-stop:visible").count() == 0)
    check("partial answer kept", len(page.inner_text(".msg.ai .bubble").strip()) > 0)

    print("\n== regenerate ==")
    page.click("#newChatBtn")
    pick_model(page, "mock/ok")
    send(page, "повтори ответ")
    page.wait_for_selector(".msg.ai .msg-acts", timeout=30000)
    before = page.locator(".msg").count()
    page.locator(".msg.ai .msg-acts button:has-text('Повторить ответ')").first.click()
    page.wait_for_selector(".msg.ai .msg-acts", timeout=30000)
    check("regenerate keeps one answer", page.locator(".msg").count() == before,
          f"{before} -> {page.locator('.msg').count()}")

    print("\n== sending a message with each of the four models ==")
    for idx, family in enumerate(["DeepSeek", "Qwen", "Llama", "Mistral"]):
        page.click("#newChatBtn")
        page.wait_for_selector(".welcome .starters .starter", timeout=10000)
        card = page.locator(".welcome .starters .starter").nth(idx)
        status = card.locator(".s-status").inner_text().strip()
        card.click()
        page.wait_for_timeout(250)
        send(page, f"Привет от {family}")
        page.wait_for_selector(".msg.ai .msg-acts", timeout=30000)
        bubble = page.locator(".msg.ai .bubble").first
        if status == "Online":
            check(f"{family} answers in the UI",
                  bubble.locator(".code-block").count() == 1
                  and "error" not in (bubble.get_attribute("class") or ""),
                  bubble.inner_text()[:70])
        else:
            check(f"{family} shows the unavailable notice",
                  "Модель сейчас недоступна через Hugging Face Inference."
                  in bubble.inner_text(), bubble.inner_text()[:90])
        check(f"{family} keeps the app usable", page.locator("#input").is_enabled())

    print("\n== persistence ==")
    model_before = page.inner_text("#modelName")
    page.reload(wait_until="networkidle")
    check("history survives reload", page.locator(".chat-item").count() >= 5,
          str(page.locator(".chat-item").count()))
    check("messages survive reload", page.locator(".msg").count() >= 2)
    check("model survives reload", page.inner_text("#modelName") == model_before,
          f"{model_before!r} -> {page.inner_text('#modelName')!r}")

    print("\n== mobile ==")
    mob = ctx.new_page()
    mob.set_viewport_size({"width": 390, "height": 844})   # iPhone-class
    mob.goto(APP, wait_until="networkidle")
    check("sidebar hidden on phone", not mob.locator("#sidebar").is_visible()
          or mob.eval_on_selector("#sidebar", "n => n.getBoundingClientRect().right <= 1"))
    check("hamburger visible", mob.locator("#menuBtn").is_visible())
    mob.click("#menuBtn")
    mob.wait_for_timeout(420)
    check("menu opens", mob.eval_on_selector("#sidebar", "n => n.getBoundingClientRect().left >= -1"))
    mob.screenshot(path=str(SHOTS / "06-mobile-menu.png"))
    mob.mouse.click(370, 430)   # the strip of scrim the drawer leaves visible
    mob.wait_for_timeout(420)
    check("menu closes", mob.eval_on_selector("#sidebar", "n => n.getBoundingClientRect().right <= 1"))
    check("no horizontal overflow",
          mob.evaluate("document.documentElement.scrollWidth <= window.innerWidth + 1"))
    mob.screenshot(path=str(SHOTS / "07-mobile.png"), full_page=True)

    tab = ctx.new_page()
    tab.set_viewport_size({"width": 820, "height": 1180})   # tablet
    tab.goto(APP, wait_until="networkidle")
    check("tablet has no overflow",
          tab.evaluate("document.documentElement.scrollWidth <= window.innerWidth + 1"))
    tab.screenshot(path=str(SHOTS / "08-tablet.png"))

    print("\n== console ==")
    real = [e for e in console_errors if "favicon" not in e.lower()]
    check("no console errors", not real, "; ".join(real[:3]))

    browser.close()

print("\n" + "=" * 46)
if failures:
    print(f"FAILED: {len(failures)} -> {failures}")
    sys.exit(1)
print("ALL FRONTEND CHECKS PASSED")
