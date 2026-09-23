"""Multi AI Chat - FastAPI backend.

The Hugging Face token lives ONLY here, in the HF_TOKEN environment variable.
It is never sent to the browser: the frontend talks to /api/chat and this
process adds the Authorization header on its way out to Hugging Face.
"""

from __future__ import annotations

import asyncio
import json
import logging
import os
import pathlib
import re
from typing import Any, AsyncIterator, Literal, NamedTuple

import httpx
from fastapi import FastAPI
from fastapi.responses import FileResponse, JSONResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field, ValidationError

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("multi-ai-chat")

BASE_DIR = pathlib.Path(__file__).parent
STATIC_DIR = BASE_DIR / "static"
MODELS_FILE = BASE_DIR / "models.json"

HF_TOKEN = (os.environ.get("HF_TOKEN") or "").strip()
HF_BASE_URL = (os.environ.get("HF_BASE_URL") or "https://router.huggingface.co/v1").rstrip("/")
HF_TIMEOUT = float(os.environ.get("HF_TIMEOUT") or 120)

# Every provider speaks the OpenAI-compatible shape, so adding one is data:
# a prefix that routes to it, a base URL and a key. "hf" has no prefix and is
# the default, which keeps plain "owner/name" ids working as before.
PROVIDERS: dict[str, dict[str, str]] = {
    "hf": {
        "prefix": "",
        "base_url": HF_BASE_URL,
        "key": HF_TOKEN,
        "label": "Hugging Face",
        "env": "HF_TOKEN",
    },
    "deepseek": {
        "prefix": "deepseek:",
        "base_url": (os.environ.get("DEEPSEEK_BASE_URL") or "https://api.deepseek.com/v1").rstrip("/"),
        "key": (os.environ.get("DEEPSEEK_API_KEY") or "").strip(),
        "label": "DeepSeek",
        "env": "DEEPSEEK_API_KEY",
    },
    "ollama": {
        "prefix": "ollama:",
        "base_url": (os.environ.get("OLLAMA_BASE_URL") or "https://ollama.com/v1").rstrip("/"),
        "key": (os.environ.get("OLLAMA_API_KEY") or "").strip(),
        "label": "Ollama",
        "env": "OLLAMA_API_KEY",
    },
}

# A Hugging Face Model ID looks like "owner/name" or, on the router, may carry
# an explicit provider suffix such as "owner/name:together".
MODEL_ID_RE = re.compile(r"^[A-Za-z0-9._\-]+/[A-Za-z0-9._\-]+(:[A-Za-z0-9._\-]+)?$")
# A prefixed id routes elsewhere. Ollama tags carry their own colon, as in
# "ollama:gpt-oss:120b-cloud", so colons are allowed after the prefix.
PREFIXED_ID_RE = re.compile(r"^(deepseek|ollama):[A-Za-z0-9._:\-]+$")


class Route(NamedTuple):
    """Where a request goes, and under whose key."""

    provider: str
    base_url: str
    key: str
    model: str
    label: str


def route_for(model_id: str) -> Route:
    for name, spec in PROVIDERS.items():
        prefix = spec["prefix"]
        if prefix and model_id.startswith(prefix):
            return Route(name, spec["base_url"], spec["key"],
                         model_id[len(prefix):], spec["label"])
    hf = PROVIDERS["hf"]
    return Route("hf", hf["base_url"], hf["key"], model_id, hf["label"])


def norm_id(provider: str, model: str) -> str:
    """How a model name is compared against a provider's listing.

    Only the Hugging Face router appends ":provider" to an id; elsewhere a
    colon is part of the tag, so stripping it would break the match.
    """
    name = model.strip().lower()
    return name.split(":", 1)[0] if provider == "hf" else name


MAX_MESSAGES = 120
MAX_CHARS = 120_000

app = FastAPI(title="Multi AI Chat", docs_url=None, redoc_url=None)


# --------------------------------------------------------------------------- #
# Schemas
# --------------------------------------------------------------------------- #
class Message(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    model: str
    messages: list[Message] = Field(min_length=1)
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    max_tokens: int = Field(default=2048, ge=1, le=32000)
    top_p: float = Field(default=0.95, gt=0.0, le=1.0)
    stream: bool = True


class ChatError(Exception):
    """An error that already carries a user-facing Russian message."""

    def __init__(self, code: str, message: str, status: int = 502) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status = status


# --------------------------------------------------------------------------- #
# Model catalogue
# --------------------------------------------------------------------------- #
def load_models() -> dict[str, Any]:
    try:
        with MODELS_FILE.open(encoding="utf-8") as fh:
            return json.load(fh)
    except Exception as exc:  # pragma: no cover - only on a broken deploy
        log.error("models.json unreadable: %s", exc)
        return {"default": "deepseek-ai/DeepSeek-V3-0324", "categories": []}


# --------------------------------------------------------------------------- #
# Error translation
# --------------------------------------------------------------------------- #
MODEL_UNAVAILABLE = "Модель сейчас недоступна через Hugging Face Inference."


def classify_http_error(status: int, body: str, provider: str = "hf") -> ChatError:
    """Map an upstream HTTP response onto a friendly Russian message."""
    low = body.lower()

    # Anything that is not Hugging Face speaks for itself, by name.
    if provider != "hf":
        who = PROVIDERS.get(provider, {}).get("label", provider)
        if status in (401, 403):
            return ChatError("bad_token", f"Неверный ключ {who}. Проверьте его в настройках.", status)
        if status == 402:
            return ChatError(
                "quota",
                f"Недостаточно средств на балансе {who}. Пополните его на platform.deepseek.com."
                if provider == "deepseek" else f"Исчерпан лимит {who}.",
                status,
            )
        if status == 429:
            return ChatError("rate_limit", f"Превышен лимит запросов {who}. Подождите немного.", status)
        if status == 404:
            return ChatError("model_unavailable", f"Такой модели нет в {who}.", status)

    if status in (401, 403):
        if "gated" in low or "awaiting approval" in low or "accept" in low and "license" in low:
            return ChatError(
                "model_gated",
                "Эта модель закрыта (gated). Откройте её страницу на Hugging Face и примите условия доступа.",
                status,
            )
        return ChatError(
            "bad_token",
            "Неверный или просроченный HF_TOKEN. Проверьте секрет на сервере.",
            status,
        )

    if status == 404:
        return ChatError("model_unavailable", MODEL_UNAVAILABLE, status)

    if status == 429:
        return ChatError(
            "rate_limit",
            "Превышен лимит запросов Hugging Face. Подождите немного и повторите.",
            status,
        )

    if status == 503 or "loading" in low or "currently loading" in low:
        return ChatError(
            "model_loading",
            "Модель загружается на стороне Hugging Face. Попробуйте ещё раз через полминуты.",
            status,
        )

    if status == 400 and ("not supported" in low or "no provider" in low or "unsupported" in low):
        return ChatError("model_unavailable", MODEL_UNAVAILABLE, status)

    detail = extract_hf_message(body)
    suffix = f" ({detail})" if detail else ""
    return ChatError(
        "hf_error",
        f"Ошибка Hugging Face {status}{suffix}",
        status,
    )


def extract_hf_message(body: str) -> str:
    try:
        data = json.loads(body)
    except Exception:
        return body.strip()[:200]
    if isinstance(data, dict):
        err = data.get("error")
        if isinstance(err, dict):
            return str(err.get("message", ""))[:200]
        if isinstance(err, str):
            return err[:200]
        if "message" in data:
            return str(data["message"])[:200]
    return body.strip()[:200]


# --------------------------------------------------------------------------- #
# Hugging Face plumbing
# --------------------------------------------------------------------------- #
def build_payload(req: ChatRequest, stream: bool, model: str | None = None) -> dict[str, Any]:
    return {
        "model": model or req.model,
        "messages": [m.model_dump() for m in req.messages],
        "temperature": req.temperature,
        "max_tokens": req.max_tokens,
        "top_p": req.top_p,
        "stream": stream,
    }


def sse(event: dict[str, Any]) -> str:
    return f"data: {json.dumps(event, ensure_ascii=False)}\n\n"


def validate(req: ChatRequest) -> Route:
    if not (MODEL_ID_RE.match(req.model) or PREFIXED_ID_RE.match(req.model)):
        raise ChatError(
            "bad_model_id",
            "Некорректный Model ID. Формат: owner/name, deepseek:имя или ollama:имя",
            400,
        )
    dest = route_for(req.model)
    if not dest.key:
        env = PROVIDERS[dest.provider]["env"]
        raise ChatError(
            "no_token",
            f"На сервере не задан ключ {dest.label} ({env}). "
            "Добавьте его в секреты хостинга и перезапустите приложение.",
            503,
        )
    if len(req.messages) > MAX_MESSAGES:
        raise ChatError("too_many_messages", "Слишком длинная история чата.", 413)
    if sum(len(m.content) for m in req.messages) > MAX_CHARS:
        raise ChatError("too_long", "Слишком длинный запрос.", 413)
    return dest


async def stream_chat(req: ChatRequest) -> AsyncIterator[str]:
    """Proxy a streaming completion, re-framed as our own SSE envelope."""
    try:
        dest = validate(req)
    except ChatError as err:
        yield sse({"type": "error", "code": err.code, "message": err.message})
        return

    headers = {
        "Authorization": f"Bearer {dest.key}",
        "Content-Type": "application/json",
        "Accept": "text/event-stream",
    }
    url = f"{dest.base_url}/chat/completions"
    produced = False

    try:
        timeout = httpx.Timeout(HF_TIMEOUT, connect=30.0)
        async with httpx.AsyncClient(timeout=timeout) as client:
            async with client.stream(
                "POST", url, headers=headers, json=build_payload(req, True, dest.model)
            ) as resp:
                if resp.status_code >= 400:
                    body = (await resp.aread()).decode("utf-8", "replace")
                    err = classify_http_error(resp.status_code, body, dest.provider)
                    log.warning("%s %s for %s: %s", dest.provider, resp.status_code,
                                req.model, body[:300])
                    yield sse({"type": "error", "code": err.code, "message": err.message})
                    return

                yield sse({"type": "start", "model": req.model})

                async for raw in resp.aiter_lines():
                    if not raw or not raw.startswith("data:"):
                        continue
                    data = raw[5:].strip()
                    if data == "[DONE]":
                        break
                    try:
                        chunk = json.loads(data)
                    except json.JSONDecodeError:
                        continue

                    if isinstance(chunk, dict) and chunk.get("error"):
                        msg = extract_hf_message(data) or MODEL_UNAVAILABLE
                        yield sse({"type": "error", "code": "hf_error", "message": msg})
                        return

                    for choice in chunk.get("choices") or []:
                        delta = choice.get("delta") or {}
                        reasoning = delta.get("reasoning_content") or delta.get("reasoning")
                        if reasoning:
                            yield sse({"type": "reasoning", "content": reasoning})
                        piece = delta.get("content")
                        if piece:
                            produced = True
                            yield sse({"type": "delta", "content": piece})

                    if chunk.get("usage"):
                        yield sse({"type": "usage", "usage": chunk["usage"]})

        if not produced:
            yield sse(
                {
                    "type": "error",
                    "code": "empty_response",
                    "message": "Модель вернула пустой ответ. Попробуйте ещё раз или выберите другую модель.",
                }
            )
            return

        yield sse({"type": "done"})

    except (httpx.ConnectTimeout, httpx.ReadTimeout, httpx.WriteTimeout, asyncio.TimeoutError):
        yield sse(
            {
                "type": "error",
                "code": "timeout",
                "message": "Время ожидания ответа истекло. Попробуйте ещё раз.",
            }
        )
    except httpx.HTTPError as exc:
        log.warning("network error: %s", exc)
        yield sse(
            {
                "type": "error",
                "code": "network",
                "message": "Ошибка сети при обращении к Hugging Face.",
            }
        )
    except asyncio.CancelledError:
        raise
    except Exception as exc:  # pragma: no cover - last-resort guard
        log.exception("unexpected stream failure")
        yield sse(
            {
                "type": "error",
                "code": "internal",
                "message": f"Внутренняя ошибка сервера: {type(exc).__name__}",
            }
        )


async def complete_once(req: ChatRequest) -> dict[str, Any]:
    """Non-streaming completion, used when the client asks for stream=false."""
    dest = validate(req)
    headers = {"Authorization": f"Bearer {dest.key}", "Content-Type": "application/json"}
    url = f"{dest.base_url}/chat/completions"
    timeout = httpx.Timeout(HF_TIMEOUT, connect=30.0)

    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            resp = await client.post(url, headers=headers,
                                     json=build_payload(req, False, dest.model))
    except (httpx.ConnectTimeout, httpx.ReadTimeout, httpx.WriteTimeout):
        raise ChatError("timeout", "Время ожидания ответа истекло.", 504)
    except httpx.HTTPError:
        raise ChatError("network", "Ошибка сети при обращении к Hugging Face.", 502)

    if resp.status_code >= 400:
        raise classify_http_error(resp.status_code, resp.text, dest.provider)

    data = resp.json()
    choices = data.get("choices") or []
    content = ""
    if choices:
        content = (choices[0].get("message") or {}).get("content") or ""
    if not content:
        raise ChatError("empty_response", "Модель вернула пустой ответ.", 502)
    return {"content": content, "usage": data.get("usage"), "model": req.model}


# --------------------------------------------------------------------------- #
# Featured models
#
# Each featured card lists several Model IDs of the same family. We ask the
# router which models it actually serves and pick the first candidate that is
# live, so a retired Model ID silently falls back to a sibling instead of
# leaving a dead card on the page.
# --------------------------------------------------------------------------- #
FEATURED_TTL = 300.0
_featured_cache: dict[str, Any] = {"at": 0.0, "data": None}


async def fetch_served_ids(client: httpx.AsyncClient, provider: str) -> set[str] | None:
    """Model IDs the provider currently serves, or None if the listing failed."""
    spec = PROVIDERS[provider]
    try:
        resp = await client.get(
            f"{spec['base_url']}/models",
            headers={"Authorization": f"Bearer {spec['key']}"},
        )
        if resp.status_code >= 400:
            log.warning("model listing returned %s", resp.status_code)
            return None
        payload = resp.json()
    except (httpx.HTTPError, ValueError) as exc:
        log.warning("model listing unavailable: %s", exc)
        return None

    rows = payload.get("data") if isinstance(payload, dict) else payload
    if not isinstance(rows, list):
        return None

    served: set[str] = set()
    for row in rows:
        ident = row.get("id") if isinstance(row, dict) else row
        if isinstance(ident, str) and ident:
            served.add(norm_id(provider, ident))
    return served or None


async def probe_model(client: httpx.AsyncClient, model_id: str) -> bool:
    """Cheapest possible call that still proves the model answers."""
    dest = route_for(model_id)
    if not dest.key:
        return False
    try:
        resp = await client.post(
            f"{dest.base_url}/chat/completions",
            headers={"Authorization": f"Bearer {dest.key}", "Content-Type": "application/json"},
            json={
                "model": dest.model,
                "messages": [{"role": "user", "content": "ping"}],
                "max_tokens": 1,
                "stream": False,
            },
        )
        return resp.status_code < 400
    except httpx.HTTPError:
        return False


async def resolve_featured(force: bool = False) -> list[dict[str, Any]]:
    cfg = load_models().get("featured") or []
    now = asyncio.get_running_loop().time()

    cached = _featured_cache["data"]
    if cached is not None and not force and now - _featured_cache["at"] < FEATURED_TTL:
        return cached

    def card(entry: dict[str, Any], model_id: str, status: str) -> dict[str, Any]:
        return {
            "key": entry.get("key", ""),
            "provider": entry.get("provider", "hf"),
            "name": entry.get("name", ""),
            "icon": entry.get("icon", ""),
            "desc": entry.get("desc", ""),
            "type": entry.get("type", "normal"),
            "id": model_id,
            "status": status,
            "candidates": entry.get("candidates") or [],
        }

    timeout = httpx.Timeout(20.0, connect=10.0)
    async with httpx.AsyncClient(timeout=timeout) as client:
        # Ask each provider that has a key which models it serves. A provider
        # with no key needs no call: its cards are offline by definition.
        listings: dict[str, set[str] | None] = {}
        for name, spec in PROVIDERS.items():
            listings[name] = await fetch_served_ids(client, name) if spec["key"] else None

        async def resolve_one(entry: dict[str, Any]) -> dict[str, Any]:
            cands = entry.get("candidates") or []
            first = cands[0] if cands else ""
            provider = entry.get("provider", "hf")
            if not first or not route_for(first).key:
                return card(entry, first, "offline")

            served = listings.get(provider)
            if served:
                chosen = next(
                    (c for c in cands if norm_id(provider, route_for(c).model) in served), None)
                if chosen:
                    return card(entry, chosen, "online")
                # Ollama's cloud tags move, so a card may opt into using
                # whatever the account does serve rather than going dark.
                if entry.get("fallbackToListing"):
                    prefix = PROVIDERS[provider]["prefix"]
                    pick = sorted(served)[0]
                    return card(entry, f"{prefix}{pick}", "online")
                return card(entry, first, "offline")

            # Listing unavailable - fall back to probing the top candidates.
            for cand in cands[:2]:
                if await probe_model(client, cand):
                    return card(entry, cand, "online")
            return card(entry, first, "offline")

        result = list(await asyncio.gather(*(resolve_one(e) for e in cfg)))

    _featured_cache.update(at=now, data=result)
    return result


# --------------------------------------------------------------------------- #
# Routes
# --------------------------------------------------------------------------- #
@app.get("/api/featured")
async def featured(refresh: bool = False) -> dict[str, Any]:
    return {"featured": await resolve_featured(force=refresh)}


@app.get("/api/health")
async def health() -> dict[str, Any]:
    # Reports only whether a token exists - never the token itself.
    return {
        "ok": True,
        "token_configured": bool(HF_TOKEN),
        "providers": {name: bool(spec["key"]) for name, spec in PROVIDERS.items()},
        "deepseek_configured": bool(PROVIDERS["deepseek"]["key"]),
        "ollama_configured": bool(PROVIDERS["ollama"]["key"]),
        "base_url": HF_BASE_URL,
    }


@app.get("/api/models")
async def models() -> dict[str, Any]:
    return load_models()


@app.post("/api/chat")
async def chat(payload: dict[str, Any]) -> Any:
    try:
        req = ChatRequest.model_validate(payload)
    except ValidationError as exc:
        first = exc.errors()[0]
        where = ".".join(str(p) for p in first.get("loc", ()))
        return JSONResponse(
            status_code=400,
            content={
                "error": {
                    "code": "bad_request",
                    "message": f"Некорректный запрос: {where} - {first.get('msg')}",
                }
            },
        )

    if req.stream:
        return StreamingResponse(
            stream_chat(req),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache, no-transform",
                "X-Accel-Buffering": "no",
                "Connection": "keep-alive",
            },
        )

    try:
        return await complete_once(req)
    except ChatError as err:
        return JSONResponse(
            status_code=err.status, content={"error": {"code": err.code, "message": err.message}}
        )


@app.get("/")
async def index() -> FileResponse:
    return FileResponse(STATIC_DIR / "index.html")


app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")
