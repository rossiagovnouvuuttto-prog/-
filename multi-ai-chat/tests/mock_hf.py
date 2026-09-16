"""A stand-in for the Hugging Face router, used to exercise app.py locally.

The model id selects the behaviour, so every error branch in the real backend
can be driven deterministically:

  mock/ok             -> normal streamed answer (markdown, code, table)
  mock/unauthorized   -> 401 invalid token
  mock/gated          -> 403 gated repo
  mock/missing        -> 404 unknown model
  mock/ratelimit      -> 429
  mock/loading        -> 503 model loading
  mock/noprovider     -> 400 no provider supports this model
  mock/midstream      -> starts streaming, then emits an error event
  mock/empty          -> streams nothing at all
  mock/slow           -> streams very slowly (for abort / timeout tests)
  mock/reasoning      -> emits reasoning_content before the answer
"""

from __future__ import annotations

import asyncio
import json
import os

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse

app = FastAPI()

# The Chrome extension calls this from its own origin, so the stand-in has
# to answer preflights the way the real router does.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

ANSWER = """# Ответ модели

Вот **пример** с `inline code`, ссылкой на [Hugging Face](https://huggingface.co) и списком:

- первый пункт
- второй пункт
  - вложенный пункт
- третий пункт

1. раз
2. два

```python
def greet(name: str) -> str:
    # приветствие
    count = 42
    return f"Привет, {name}! {count}"
```

| Модель | Провайдер | Скорость |
| --- | --- | --- |
| DeepSeek V3 | HF | быстро |
| Qwen2.5 | HF | средне |

> Цитата для проверки блока.

Готово.
"""


def chunk(delta: dict) -> str:
    payload = {
        "id": "chatcmpl-mock",
        "object": "chat.completion.chunk",
        "choices": [{"index": 0, "delta": delta, "finish_reason": None}],
    }
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


ERRORS = {
    "mock/unauthorized": (401, {"error": {"message": "Invalid credentials in Authorization header"}}),
    "mock/gated": (403, {"error": {"message": "This repo is gated, you must accept the license"}}),
    "mock/missing": (404, {"error": {"message": "Model not found"}}),
    "mock/ratelimit": (429, {"error": {"message": "Rate limit reached"}}),
    "mock/loading": (503, {"error": {"message": "Model is currently loading"}}),
    "mock/noprovider": (400, {"error": {"message": "No provider supports this model for chat"}}),
}


# Models the fake router "serves". Chosen so featured resolution is exercised
# in all three shapes: first candidate live (DeepSeek, Mistral), first candidate
# retired so the family falls back (Qwen), and nothing live at all (Llama).
SERVED = {
    "deepseek-ai/DeepSeek-V3-0324",
    "Qwen/Qwen2.5-7B-Instruct",
    "mistralai/Mistral-7B-Instruct-v0.3",
    "google/gemma-2-9b-it",
    "mock/ok",
    "mock/slow",
    "mock/reasoning",
}


@app.get("/v1/models")
async def models():
    # MOCK_NO_LISTING exercises the probe-based fallback in app.py.
    if os.environ.get("MOCK_NO_LISTING"):
        return JSONResponse(status_code=500, content={"error": {"message": "listing down"}})
    return {"object": "list", "data": [{"id": m, "object": "model"} for m in sorted(SERVED)]}


# ---------------------------------------------------------------------------
# DeepSeek's own API, served under /ds/v1 so one process stands in for both
# providers. It wants an "sk-" key, which is how a mixed-up key is caught.
# ---------------------------------------------------------------------------
DS_SERVED = {"deepseek-chat", "deepseek-reasoner"}


@app.get("/ds/v1/models")
async def ds_models():
    if os.environ.get("MOCK_DS_NO_LISTING"):
        return JSONResponse(status_code=500, content={"error": {"message": "listing down"}})
    return {"object": "list", "data": [{"id": m, "object": "model"} for m in sorted(DS_SERVED)]}


@app.post("/ds/v1/chat/completions")
async def ds_completions(request: Request):
    body = await request.json()
    model = body.get("model", "")
    auth = request.headers.get("authorization", "")

    if not auth.startswith("Bearer sk-"):
        return JSONResponse(status_code=401, content={"error": {"message": "Invalid API key"}})
    if os.environ.get("MOCK_DS_NO_BALANCE"):
        return JSONResponse(status_code=402, content={"error": {"message": "Insufficient Balance"}})
    if model not in DS_SERVED:
        return JSONResponse(status_code=404, content={"error": {"message": "Model Not Exist"}})

    if not body.get("stream"):
        return {
            "choices": [{"message": {"role": "assistant", "content": ANSWER}}],
            "usage": {"prompt_tokens": 9, "completion_tokens": 240, "total_tokens": 249},
        }
    return StreamingResponse(stream_answer(model), media_type="text/event-stream")


async def stream_answer(model: str):
    """The streamed reply shared by both providers."""
    if model == "mock/empty":
        yield "data: [DONE]\n\n"
        return

    if model == "mock/reasoning":
        for piece in ["Думаю над ответом. ", "Проверяю факты. "]:
            yield chunk({"reasoning_content": piece})
            await asyncio.sleep(0.01)

    step = 24
    for i in range(0, len(ANSWER), step):
        yield chunk({"content": ANSWER[i:i + step]})
        await asyncio.sleep(0.5 if model == "mock/slow" else 0.005)

        if model == "mock/midstream" and i >= step * 2:
            yield f"data: {json.dumps({'error': {'message': 'Upstream provider exploded'}})}\n\n"
            return

    yield f"data: {json.dumps({'usage': {'total_tokens': 252}})}\n\n"
    yield "data: [DONE]\n\n"


@app.post("/v1/chat/completions")
async def completions(request: Request):
    body = await request.json()
    model = body.get("model", "")
    auth = request.headers.get("authorization", "")

    if not auth.startswith("Bearer ") or len(auth) < 12:
        return JSONResponse(status_code=401, content={"error": {"message": "Missing token"}})

    if model in ERRORS:
        status, payload = ERRORS[model]
        return JSONResponse(status_code=status, content=payload)

    # Anything the fake router does not serve behaves like a retired Model ID,
    # so probe-based featured resolution can tell live models from dead ones.
    if model not in SERVED and not model.startswith("mock/"):
        return JSONResponse(status_code=404, content={"error": {"message": "Model not found"}})

    if not body.get("stream"):
        return {
            "choices": [{"message": {"role": "assistant", "content": ANSWER}}],
            "usage": {"prompt_tokens": 12, "completion_tokens": 240, "total_tokens": 252},
        }

    return StreamingResponse(stream_answer(model), media_type="text/event-stream")
