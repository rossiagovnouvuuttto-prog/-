"""End-to-end checks against a running app.py (pointed at tests/mock_hf.py).

Run:
    uvicorn tests.mock_hf:app --port 8899 &
    HF_TOKEN=hf_fake... HF_BASE_URL=http://127.0.0.1:8899/v1 uvicorn app:app --port 8800 &
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
    "mock/unauthorized": ("bad_token", "HF_TOKEN"),
    "mock/gated": ("model_gated", "gated"),
    "mock/missing": ("model_unavailable", "Hugging Face Inference"),
    "mock/ratelimit": ("rate_limit", "лимит"),
    "mock/loading": ("model_loading", "загружается"),
    "mock/noprovider": ("model_unavailable", "Hugging Face Inference"),
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
ev = post_stream(base("not-a-valid-id"))
err = next((e for e in ev if e.get("type") == "error"), None)
check("rejects bad model id", bool(err) and err.get("code") == "bad_model_id")

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

check("returns five cards", len(cards) == 5, str(len(cards)))
check("families are the requested ones",
      [c["name"] for c in cards] == ["DeepSeek", "Qwen", "Llama", "Mistral", "DeepSeek API"],
      str([c["name"] for c in cards]))
check("every card has an icon", all(c["icon"] for c in cards))
check("every card has a description", all(c["desc"] for c in cards))
check("every card has a Model ID",
      all("/" in c["id"] or c["id"].startswith("deepseek:") for c in cards),
      str([c["id"] for c in cards]))
check("status is online or offline",
      all(c["status"] in ("online", "offline") for c in cards),
      str([c["status"] for c in cards]))

by_name = {c["name"]: c for c in cards}
check("live first choice is kept",
      by_name["DeepSeek"]["id"] == "deepseek-ai/DeepSeek-V3-0324"
      and by_name["DeepSeek"]["status"] == "online",
      json.dumps(by_name["DeepSeek"], ensure_ascii=False))
check("retired ID falls back inside the same family",
      by_name["Qwen"]["id"] == "Qwen/Qwen2.5-7B-Instruct"
      and by_name["Qwen"]["status"] == "online",
      json.dumps(by_name["Qwen"], ensure_ascii=False))
check("family with nothing served reads offline",
      by_name["Llama"]["status"] == "offline",
      json.dumps(by_name["Llama"], ensure_ascii=False))
check("resolved model actually answers",
      by_name["Mistral"]["status"] == "online",
      json.dumps(by_name["Mistral"], ensure_ascii=False))

print("\n== the DeepSeek API provider ==")
ds = by_name["DeepSeek API"]
check("card is routed to deepseek", ds["provider"] == "deepseek", json.dumps(ds, ensure_ascii=False))
check("card resolved to a prefixed id", ds["id"].startswith("deepseek:"), ds["id"])
check("card is online with a key set", ds["status"] == "online", ds["status"])
check("health reports the deepseek key", health_pre.get("deepseek_configured") is True,
      json.dumps(health_pre))

ev = post_stream(base(ds["id"]))
text = "".join(e.get("content", "") for e in ev if e.get("type") == "delta")
check("DeepSeek API answers", len(text) > 50 and not any(e.get("type") == "error" for e in ev),
      str([e.get("type") for e in ev][:4]))

ev = post_stream(base("deepseek:no-such-model"))
err = next((e for e in ev if e.get("type") == "error"), None)
check("unknown DeepSeek model is named as such",
      bool(err) and "DeepSeek API" in err.get("message", ""),
      json.dumps(err, ensure_ascii=False) if err else "none")

ev = post_stream(base("deepseek:bad id"))
err = next((e for e in ev if e.get("type") == "error"), None)
check("malformed deepseek id rejected", bool(err) and err.get("code") == "bad_model_id",
      json.dumps(err, ensure_ascii=False) if err else "none")

print("\n== chatting with each resolved model ==")
for name in ("DeepSeek", "Qwen", "Mistral"):
    ev = post_stream(base(by_name[name]["id"]))
    text = "".join(e.get("content", "") for e in ev if e.get("type") == "delta")
    check(f"{name} answers", len(text) > 50 and not any(e.get("type") == "error" for e in ev),
          str([e.get("type") for e in ev][:4]))

ev = post_stream(base(by_name["Llama"]["id"]))
err = next((e for e in ev if e.get("type") == "error"), None)
check("offline family degrades gracefully",
      bool(err) and "Hugging Face Inference" in err.get("message", ""),
      json.dumps(err, ensure_ascii=False) if err else "no error event")

print("\n== secrets are not exposed ==")
with urllib.request.urlopen(APP + "/api/health", timeout=20) as resp:
    health = json.load(resp)
check("health reports token flag only", health.get("token_configured") is True
      and not any("hf_" in str(v) for v in health.values()), json.dumps(health))

for path in ("/", "/static/app.js", "/static/styles.css"):
    with urllib.request.urlopen(APP + path, timeout=20) as resp:
        body = resp.read().decode("utf-8", "replace")
    check(f"no token in {path}", "hf_faketokenfortesting" not in body and "HF_TOKEN" not in body
          or path == "/static/app.js" and "hf_faketokenfortesting" not in body)

print("\n" + ("=" * 46))
if failures:
    print(f"FAILED: {len(failures)} -> {failures}")
    sys.exit(1)
print("ALL BACKEND CHECKS PASSED")
