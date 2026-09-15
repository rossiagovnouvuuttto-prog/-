---
title: Multi AI Chat
emoji: 💬
colorFrom: blue
colorTo: purple
sdk: docker
app_port: 7860
pinned: false
short_description: Чат с нейросетями Hugging Face — DeepSeek, Qwen, Llama, Mistral, Gemma
---

# Multi AI Chat

Веб-чат с моделями Hugging Face: выбираете нейросеть и общаетесь. Тёмный интерфейс,
потоковые ответы, Markdown с подсветкой кода, история чатов, настройки генерации.

```
Браузер  ──POST /api/chat──▶  FastAPI (app.py)  ──Bearer HF_TOKEN──▶  Hugging Face Router
   ▲                                │
   └────────── SSE поток ───────────┘
```

Токен живёт только в процессе сервера. Фронтенд его не видит и не может увидеть:
браузер обращается только к `/api/chat`, заголовок `Authorization` добавляет backend.

## Возможности

- **Выбор модели** — 22 модели в 7 категориях + любая своя по Hugging Face Model ID
- **Стриминг** — ответ печатается по мере генерации, есть кнопка «Остановить»
- **Markdown** — заголовки, списки, таблицы, цитаты, ссылки, блоки кода с подсветкой
  и кнопкой «Копировать код» (без внешних библиотек)
- **Под ответом** — Копировать · Повторить ответ · 👍 · 👎
- **История** — создать, открыть, переименовать, удалить чат, очистить всё (localStorage)
- **Настройки** — System Prompt, Temperature, Max Tokens, Top P
- **Адаптив** — телефон, планшет, ПК; на телефоне меню открывается через ☰
- **Ошибки** — неверный токен, недоступная модель, загрузка модели, rate limit,
  таймаут, обрыв сети: сайт продолжает работать и показывает уведомление

## Где хранится HF_TOKEN

В переменной окружения `HF_TOKEN` на сервере — и только там.

| Площадка | Куда добавить |
| --- | --- |
| Hugging Face Space | Settings → Variables and secrets → **New secret** → `HF_TOKEN` |
| GitHub Actions (автодеплой) | Settings → Secrets and variables → Actions → `HF_TOKEN` |
| Локально | `export HF_TOKEN=hf_...` или файл `.env` (см. `.env.example`) |

Токен не попадает ни в HTML, ни в JavaScript, ни в репозиторий. Проверка:
`GET /api/health` возвращает только флаг `{"token_configured": true}` — само значение
наружу не отдаётся никогда. Это закреплено тестом в `tests/test_backend.py`.

## Как добавить новую модель

**Способ 1 — прямо на сайте.** «Выбрать модель» → внизу поле «Добавить свою модель
по Hugging Face Model ID» → вставьте ID вида `owner/name` → «Добавить». Модель
сохраняется в браузере и появляется в категории «Мои модели».

**Способ 2 — в каталоге.** Добавьте запись в `models.json`, в нужную категорию:

```json
{
  "id": "deepseek-ai/DeepSeek-V3-0324",
  "name": "DeepSeek V3 0324",
  "desc": "Короткое описание"
}
```

Обязателен только `id`. После изменения перезапустите приложение (или сделайте
redeploy Space). Новую категорию можно добавить в массив `categories`.

Если модель не обслуживается ни одним inference-провайдером Hugging Face, сайт не
ломается — он показывает: **«Модель сейчас недоступна через Hugging Face Inference.»**

## Модель по умолчанию

`deepseek-ai/DeepSeek-V3-0324` — задаётся полем `default` в `models.json`.

## Запуск локально

```bash
pip install -r requirements.txt
export HF_TOKEN=hf_...
uvicorn app:app --reload --port 7860
# http://127.0.0.1:7860
```

Через Docker:

```bash
docker build -t multi-ai-chat .
docker run -p 7860:7860 -e HF_TOKEN=hf_... multi-ai-chat
```

## API

| Метод | Путь | Назначение |
| --- | --- | --- |
| `GET` | `/api/health` | Статус сервера и факт наличия токена |
| `GET` | `/api/models` | Каталог моделей из `models.json` |
| `POST` | `/api/chat` | Запрос к модели, по умолчанию потоковый (SSE) |

`POST /api/chat` принимает:

```json
{
  "model": "deepseek-ai/DeepSeek-V3-0324",
  "messages": [{ "role": "user", "content": "Привет" }],
  "temperature": 0.7,
  "max_tokens": 2048,
  "top_p": 0.95,
  "stream": true
}
```

и отдаёт поток событий `data: {...}` с типами `start`, `delta`, `reasoning`,
`usage`, `done`, `error`. У `error` есть машиночитаемый `code` (`bad_token`,
`model_unavailable`, `model_loading`, `rate_limit`, `timeout`, `network`, …)
и готовое к показу сообщение на русском.

## Переменные окружения

| Имя | По умолчанию | Назначение |
| --- | --- | --- |
| `HF_TOKEN` | — | **Обязательна.** Токен Hugging Face |
| `HF_BASE_URL` | `https://router.huggingface.co/v1` | Точка входа inference |
| `HF_TIMEOUT` | `120` | Таймаут запроса, секунд |

## Тесты

`tests/mock_hf.py` подменяет Hugging Face и умеет воспроизводить каждую ошибку,
поэтому все ветки проверяются без обращения к сети.

```bash
uvicorn tests.mock_hf:app --port 8899 &
HF_TOKEN=hf_fake HF_BASE_URL=http://127.0.0.1:8899/v1 uvicorn app:app --port 8800 &

python3 tests/test_backend.py    # API, стриминг, карта ошибок, отсутствие утечки токена
python3 tests/test_frontend.py   # реальный Chromium: UI, Markdown, история, адаптив
```

## Структура

```
app.py              backend: прокси к Hugging Face, SSE, обработка ошибок
models.json         каталог моделей (правится руками)
static/index.html   разметка
static/styles.css   тёмная тема, адаптив
static/app.js       состояние, Markdown-рендер, стриминг, UI
tests/              mock Hugging Face + проверки backend и браузера
Dockerfile          образ для Hugging Face Spaces (порт 7860)
```
