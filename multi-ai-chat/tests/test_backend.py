"""End-to-end checks against a running app.py (pointed at tests/mock_hf.py).

Run:
    uvicorn tests.mock_hf:app --port 8899 &
    OLLAMA_API_KEY=test_key_1234567890 OLLAMA_BASE_URL=http://127.0.0.1:8899/v1 \
      uvicorn app:app --port 8800 &
    python3 tests/test_backend.py
"""

from __future__ import annotations

import json
import os
import sys
import urllib.request

APP = os.environ.get("APP_URL", "http://127.0.0.1:8800")
failures: list[str] = []


def post_stream(payload: dict) -> list[dict]:
    req = urllib.request.Request(
        APP + "/api/chat",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
    )
    events: list[dict] = []
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            for raw in resp:
                line = raw.decode("utf-8").strip()
                if line.startswith("data:"):
                    events.append(json.loads(line[5:].strip()))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode()
        events.append({"type": "http_error", "status": exc.code, "body": json.loads(body)})
    return events


def base(model: str, **kw) -> dict:
    payload = {
        "model": model,
        "messages": [{"role": "user", "content": "привет"}],
        "temperature": 0.7,
        "max_tokens": 512,
        "top_p": 0.95,
        "stream": True,
    }
    payload.update(kw)
    return payload


def check(name: str, ok: bool, detail: str = "") -> None:
    mark = "PASS" if ok else "FAIL"
    print(f"  [{mark}] {name}" + (f"  -> {detail}" if detail and not ok else ""))
    if not ok:
        failures.append(name)


print("\n== streaming happy path ==")
ev = post_stream(base("mock/ok"))
kinds = [e.get("type") for e in ev]
text = "".join(e.get("content", "") for e in ev if e.get("type") == "delta")
check("emits start", kinds[0] == "start", str(kinds[:3]))
check("emits deltas", kinds.count("delta") > 5, f"{kinds.count('delta')} deltas")
check("ends with done", kinds[-1] == "done", str(kinds[-3:]))
check("no error events", "error" not in kinds, str(set(kinds)))
check("reassembles markdown", "```python" in text and "| Модель |" in text, text[:80])

print("\n== reasoning passthrough ==")
ev = post_stream(base("mock/reasoning"))
check("emits reasoning", any(e.get("type") == "reasoning" for e in ev))
check("still emits answer", any(e.get("type") == "delta" for e in ev))

print("\n== upstream error mapping ==")
CASES = {
    "mock/unauthorized": ("bad_token", "ключ Ollama"),
    "mock/missing": ("model_unavailable", "недоступна в Ollama"),
    "mock/ratelimit": ("rate_limit", "лимит"),
    "mock/loading": ("model_loading", "загружается"),
    "mock/nobalance": ("quota", "лимит Ollama"),
}
for model, (code, needle) in CASES.items():
    ev = post_stream(base(model))
    err = next((e for e in ev if e.get("type") == "error"), None)
    ok = bool(err) and err.get("code") == code and needle.lower() in err.get("message", "").lower()
    check(f"{model} -> {code}", ok, json.dumps(err, ensure_ascii=False) if err else "no error event")

print("\n== mid-stream failure ==")
ev = post_stream(base("mock/midstream"))
check("keeps partial text", any(e.get("type") == "delta" for e in ev))
check("then reports error", any(e.get("type") == "error" for e in ev))

print("\n== empty completion ==")
ev = post_stream(base("mock/empty"))
err = next((e for e in ev if e.get("type") == "error"), None)
check("empty -> error event", bool(err) and err.get("code") == "empty_response",
      json.dumps(err, ensure_ascii=False) if err else "none")

print("\n== request validation ==")
ev = post_stream(base("бракованное имя"))
err = next((e for e in ev if e.get("type") == "error"), None)
check("rejects bad model id", bool(err) and err.get("code") == "bad_model_id",
      json.dumps(err, ensure_ascii=False) if err else "none")

ev = post_stream(base("mock/ok", temperature=9.5))
check("rejects out-of-range temperature",
      ev and ev[0].get("type") == "http_error" and ev[0]["status"] == 400,
      json.dumps(ev[0], ensure_ascii=False) if ev else "none")

ev = post_stream({"model": "mock/ok", "messages": []})
check("rejects empty messages",
      ev and ev[0].get("type") == "http_error" and ev[0]["status"] == 400)

print("\n== non-streaming mode ==")
req = urllib.request.Request(
    APP + "/api/chat",
    data=json.dumps(base("mock/ok", stream=False)).encode(),
    headers={"Content-Type": "application/json"},
)
with urllib.request.urlopen(req, timeout=60) as resp:
    data = json.load(resp)
check("returns content", bool(data.get("content")))
check("returns usage", bool(data.get("usage")))

print("\n== featured starter cards ==")
with urllib.request.urlopen(APP + "/api/health", timeout=20) as resp:
    health_pre = json.load(resp)
with urllib.request.urlopen(APP + "/api/featured", timeout=60) as resp:
    cards = json.load(resp)["featured"]

check("returns four cards", len(cards) == 4, str(len(cards)))
check("every card has an icon", all(c["icon"] for c in cards))
check("every card has a description", all(c["desc"] for c in cards))
check("every card has a model name", all(c["id"] for c in cards), str([c["id"] for c in cards]))
check("status is online or offline",
      all(c["status"] in ("online", "offline") for c in cards),
      str([c["status"] for c in cards]))
check("health reports the key", health_pre.get("token_configured") is True, json.dumps(health_pre))

by_name = {c["name"]: c for c in cards}
check("live first choice is kept",
      by_name["GPT-OSS"]["id"] == "gpt-oss:120b-cloud" and by_name["GPT-OSS"]["status"] == "online",
      json.dumps(by_name["GPT-OSS"], ensure_ascii=False))
check("a retired tag falls back to a sibling",
      by_name["Qwen3 Coder"]["id"] == "qwen3-coder:30b-cloud"
      and by_name["Qwen3 Coder"]["status"] == "online",
      json.dumps(by_name["Qwen3 Coder"], ensure_ascii=False))
check("a card with nothing served reads offline",
      by_name["DeepSeek"]["status"] == "offline",
      json.dumps(by_name["DeepSeek"], ensure_ascii=False))
check("tags keep their colon",
      all(":" in c["id"] for c in cards), str([c["id"] for c in cards]))

print("\n== chatting with each resolved model ==")
for name in ("GPT-OSS", "Qwen3 Coder", "GPT-OSS 20B"):
    ev = post_stream(base(by_name[name]["id"]))
    text = "".join(e.get("content", "") for e in ev if e.get("type") == "delta")
    check(f"{name} answers", len(text) > 50 and not any(e.get("type") == "error" for e in ev),
          str([e.get("type") for e in ev][:4]))

ev = post_stream(base(by_name["DeepSeek"]["id"]))
err = next((e for e in ev if e.get("type") == "error"), None)
check("an unserved model degrades gracefully",
      bool(err) and "Ollama" in err.get("message", ""),
      json.dumps(err, ensure_ascii=False) if err else "no error event")

print("\n== secrets are not exposed ==")
with urllib.request.urlopen(APP + "/api/health", timeout=20) as resp:
    health = json.load(resp)
check("health reports the flag only, never the key",
      health.get("token_configured") is True
      and not any("ollama_test_key" in str(v) for v in health.values()), json.dumps(health))

for path in ("/", "/static/app.js", "/static/styles.css"):
    with urllib.request.urlopen(APP + path, timeout=20) as resp:
        body = resp.read().decode("utf-8", "replace")
    check(f"no key in {path}", "ollama_test_key" not in body)

print("\n" + ("=" * 46))
if failures:
    print(f"FAILED: {len(failures)} -> {failures}")
    sys.exit(1)
print("ALL BACKEND CHECKS PASSED")
