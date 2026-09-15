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

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, StreamingResponse

app = FastAPI()

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

    if not body.get("stream"):
        return {
            "choices": [{"message": {"role": "assistant", "content": ANSWER}}],
            "usage": {"prompt_tokens": 12, "completion_tokens": 240, "total_tokens": 252},
        }

    async def gen():
        if model == "mock/empty":
            yield "data: [DONE]\n\n"
            return

        if model == "mock/reasoning":
            for piece in ["Думаю над ответом. ", "Проверяю факты. "]:
                yield chunk({"reasoning_content": piece})
                await asyncio.sleep(0.01)

        text = ANSWER
        step = 24
        for i in range(0, len(text), step):
            yield chunk({"content": text[i:i + step]})
            await asyncio.sleep(0.5 if model == "mock/slow" else 0.005)

            if model == "mock/midstream" and i >= step * 2:
                yield f"data: {json.dumps({'error': {'message': 'Upstream provider exploded'}})}\n\n"
                return

        yield f"data: {json.dumps({'usage': {'total_tokens': 252}})}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(gen(), media_type="text/event-stream")
