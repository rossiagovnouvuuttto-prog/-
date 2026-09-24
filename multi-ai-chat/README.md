---
title: Multi AI Chat
emoji: ⚡
colorFrom: yellow
colorTo: red
sdk: docker
app_port: 7860
pinned: false
short_description: Чат с нейросетями Ollama Cloud — GPT-OSS, Qwen3 Coder, DeepSeek, GLM
---

# Multi AI Chat

Веб-чат с моделями Ollama Cloud: выбираете нейросеть и общаетесь. Яркий игровой
интерфейс в стиле Pokémon, потоковые ответы, Markdown с подсветкой кода, история
чатов, настройки генерации.

```
Браузер  ──POST /api/chat──▶  FastAPI (app.py)  ──Bearer OLLAMA_API_KEY──▶  ollama.com
   ▲                                │
   └────────── SSE поток ───────────┘
```

Ключ живёт только в процессе сервера. Фронтенд его не видит и не может увидеть:
браузер обращается только к `/api/chat`, заголовок `Authorization` добавляет backend.

## Возможности

- **Четыре карточки на старте** — GPT-OSS, Qwen3 Coder, DeepSeek и GPT-OSS 20B:
  иконка, название, краткое описание и живой статус Online / Offline.
  Один клик выбирает модель для чата
- **Выбор модели** — каталог моделей Ollama Cloud + любая своя по Model ID
- **Стриминг** — ответ печатается по мере генерации, есть кнопка «Остановить»
- **Markdown** — заголовки, списки, таблицы, цитаты, ссылки, блоки кода с подсветкой
  и кнопкой «Копировать код» (без внешних библиотек)
- **Под ответом** — Копировать · Повторить ответ · 👍 · 👎
- **История** — создать, открыть, переименовать, удалить чат, очистить всё (localStorage)
- **Настройки** — System Prompt, Temperature, Max Tokens, Top P
- **Адаптив** — телефон, планшет, ПК; на телефоне меню открывается через ☰
- **Ошибки** — неверный ключ, недоступная модель, загрузка модели, rate limit,
  исчерпанный лимит, таймаут, обрыв сети: сайт продолжает работать и показывает
  уведомление
- **Пять стилей оформления** — Pokémon, Аниме, Minecraft, Roblox, GTA SA.
  Переключаются в «Настройках», выбор сохраняется. Каждый стиль меняет палитру,
  скругления, логотип, а Minecraft ещё и шрифт на моноширинный
- **Оформление** — яркие карточки по типам, объёмные кнопки, плавные анимации;
  текст везде контрастный (тёмный на ярком, белый на тёмном)

## Как устроены стили

Вся раскраска идёт через CSS-переменные в `:root` — цвета, радиусы, шрифт,
цвет обводки, фоны карточек `--c1…--c4`. Стиль — это блок
`:root[data-theme=...]`, который переопределяет эти переменные, поэтому новый
стиль добавляется одной палитрой, без правок разметки.

Выбранный стиль пишется в `data-theme` на `<html>` ещё до первой отрисовки,
так что при загрузке нет вспышки чужой темы. Добавить свой стиль: запись в
массив `THEMES` в `static/app.js`, блок `:root[data-theme=...]` в
`static/styles.css` и полоска-образец `.themes button[data-t=...] .t-swatch`.

## Провайдер

Один провайдер — **Ollama Cloud**, OpenAI-совместимый эндпоинт
`https://ollama.com/v1`. Model ID — обычный тег Ollama, с двоеточием:

```
gpt-oss:120b-cloud
qwen3-coder:480b-cloud
deepseek-v3.1:671b-cloud
```

Статус карточек опрашивается тем же ключом: нет ключа — карточки офлайн,
лишних запросов не делается. Ошибки называют причину словами: 402 читается как
«исчерпан лимит», 401 — «неверный ключ», а не как голый код.

Теги облачных моделей Ollama со временем меняются, поэтому у карточки GPT-OSS
стоит `fallbackToListing` — если ни один кандидат не совпал, берётся любая
модель, которую аккаунт реально отдаёт, вместо мёртвой карточки.

## Как выбирается рабочая модель

У каждой из четырёх карточек в `models.json` лежит не один Model ID, а список
`candidates` — модели одного семейства. При запросе `/api/featured` backend
спрашивает у Ollama список обслуживаемых моделей и берёт **первый кандидат,
который реально работает**:

```
GPT-OSS     : gpt-oss:120b-cloud → gpt-oss:20b-cloud → любая живая модель аккаунта
Qwen3 Coder : qwen3-coder:480b-cloud → qwen3-coder:30b-cloud
DeepSeek    : deepseek-v3.1:671b-cloud → deepseek-r1:671b-cloud
GPT-OSS 20B : gpt-oss:20b-cloud
```

Если ни один кандидат не обслуживается, карточка честно показывает **Offline**,
а попытка написать в неё выдаёт обычное сообщение об ошибке — сайт не ломается.
Результат кешируется на 5 минут; `/api/featured?refresh=true` пересчитывает его
сразу. Когда список моделей недоступен, backend переходит на запасной путь и
проверяет кандидатов пробным запросом.

## Где хранится ключ Ollama

В переменной окружения `OLLAMA_API_KEY` на сервере — и только там.
Взять ключ: [ollama.com/settings/keys](https://ollama.com/settings/keys).

| Площадка | Куда добавить |
| --- | --- |
| Cloudflare Worker | Settings → Variables and Secrets → тип **Secret** → `OLLAMA_API_KEY` |
| Hugging Face Space | Settings → Variables and secrets → **New secret** → `OLLAMA_API_KEY` |
| GitHub Actions (автодеплой) | Settings → Secrets and variables → Actions → `OLLAMA_API_KEY` |
| Расширение Chrome | Настройки в чате → поле «Ключ Ollama» (хранилище браузера) |
| Локально | `export OLLAMA_API_KEY=...` или файл `.env` (см. `.env.example`) |

Ключ не попадает ни в HTML, ни в JavaScript, ни в репозиторий. Проверка:
`GET /api/health` возвращает только флаг `{"token_configured": true}` — само значение
наружу не отдаётся никогда. Это закреплено тестом в `tests/test_backend.py`.

У расширения сервера нет вообще, поэтому ключ там лежит в `chrome.storage.local`
этого браузера и уходит только на `ollama.com`.

## Как добавить новую модель

**Способ 1 — прямо на сайте.** «Выбрать модель» → внизу поле «Добавить свою модель
по Model ID» → вставьте тег вида `name:tag` → «Добавить». Модель сохраняется в
браузере и появляется в категории «Мои модели».

**Способ 2 — в каталоге.** Добавьте запись в `models.json`, в нужную категорию:

```json
{
  "id": "glm-4.6:cloud",
  "name": "GLM 4.6",
  "desc": "Короткое описание"
}
```

Обязателен только `id`. После изменения перезапустите приложение.
Новую категорию можно добавить в массив `categories`.

**Способ 3 — новая карточка.** Добавьте элемент в массив `featured`, указав `name`,
`icon`, `desc`, `type` (`electric` / `fire` / `water` / `grass` — задаёт цвет карточки)
и список `candidates` с Model ID одного семейства.

Если модель не обслуживается аккаунтом, сайт не ломается — он показывает:
**«Эта модель сейчас недоступна в Ollama. Выберите другую карточку.»**

## Модель по умолчанию

`gpt-oss:120b-cloud` — задаётся полем `default` в `models.json`.

## Запуск локально

```bash
pip install -r requirements.txt
export OLLAMA_API_KEY=...
uvicorn app:app --reload --port 7860
# http://127.0.0.1:7860
```

Через Docker:

```bash
docker build -t multi-ai-chat .
docker run -p 7860:7860 -e OLLAMA_API_KEY=... multi-ai-chat
```

## Расширение для Chrome (без хостинга вообще)

Третья сборка: тот же чат как расширение браузера. Ни аккаунтов, ни деплоя —
работает локально, ключ хранится в хранилище расширения.

1. Распакуйте `multi-ai-chat-extension.zip`.
2. Откройте `chrome://extensions`, включите **Режим разработчика** справа вверху.
3. **Загрузить распакованное расширение** → выберите распакованную папку.
4. Нажмите на иконку покебола на панели → откроется чат.
5. **Настройки** → поле **Ключ Ollama** → вставьте ключ с ollama.com.

`static/app.js` и `static/styles.css` копируются из `static/` без единого
изменения, поэтому интерфейс тот же. Роль backend играет `static/api.js`: он
перехватывает те же запросы `/api/*` прямо в странице и сам ходит в Ollama.
`app.js` по-прежнему вызывает `fetch('/api/chat')` и ключа не видит.

Расширение обращается к API напрямую из страницы, поэтому домен провайдера
должен быть в `host_permissions` манифеста — иначе браузер заблокирует запрос
ещё до отправки. Это закреплено проверкой в `tests/test_extension.py`: локальный
стенд отвечает разрешающими CORS-заголовками и такую ошибку скрыл бы.

Пересборка после правок в `static/`:

```bash
python3 extension/build_extension.py
```

## Публикация на Cloudflare Workers (без Git и без оплаты)

Вторая сборка: тот же сайт целиком (фронтенд + backend) упакован в один файл
`worker/worker.js`, который вставляется прямо в браузере.

1. Зарегистрируйтесь на [cloudflare.com](https://dash.cloudflare.com/sign-up).
2. **Compute (Workers)** → **Create** → **Start from Hello World** → задайте имя
   `multi-ai-chat` → **Deploy**.
3. Откройте **Edit code**, выделите весь пример и вставьте содержимое `worker/worker.js`
   → **Deploy**.
4. **Settings** → **Variables and Secrets** → **Add** → тип **Secret**,
   имя `OLLAMA_API_KEY`, значение — ключ с ollama.com → **Deploy**.

Сайт открывается по адресу `https://multi-ai-chat.<ваш-поддомен>.workers.dev`.

Ключ и здесь остаётся только на сервере: он хранится в секретах Worker, а браузер
общается исключительно с `/api/chat`.

Файл `worker.js` собирается из тех же `static/` и `models.json`:

```bash
python3 worker/build_worker.py
```

Локальная проверка:

```bash
cd worker && npx wrangler dev --port 8790      # переменные берутся из .dev.vars
APP_URL=http://127.0.0.1:8790 python3 ../tests/test_backend.py
```

## API

| Метод | Путь | Назначение |
| --- | --- | --- |
| `GET` | `/api/health` | Статус сервера и факт наличия ключа |
| `GET` | `/api/models` | Каталог моделей из `models.json` |
| `GET` | `/api/featured` | Четыре карточки с подобранным Model ID и статусом |
| `POST` | `/api/chat` | Запрос к модели, по умолчанию потоковый (SSE) |

`POST /api/chat` принимает:

```json
{
  "model": "gpt-oss:120b-cloud",
  "messages": [{ "role": "user", "content": "Привет" }],
  "temperature": 0.7,
  "max_tokens": 2048,
  "top_p": 0.95,
  "stream": true
}
```

и отдаёт поток событий `data: {...}` с типами `start`, `delta`, `reasoning`,
`usage`, `done`, `error`. У `error` есть машиночитаемый `code` (`bad_token`,
`model_unavailable`, `model_loading`, `rate_limit`, `quota`, `timeout`,
`network`, …) и готовое к показу сообщение на русском.

## Переменные окружения

| Имя | По умолчанию | Назначение |
| --- | --- | --- |
| `OLLAMA_API_KEY` | — | **Обязательна.** Ключ Ollama Cloud |
| `OLLAMA_BASE_URL` | `https://ollama.com/v1` | Точка входа API |
| `OLLAMA_TIMEOUT` | `120` | Таймаут запроса, секунд |

## Тесты

`tests/mock_hf.py` подменяет Ollama и умеет воспроизводить каждую ошибку,
поэтому все ветки проверяются без обращения к сети.

```bash
uvicorn tests.mock_hf:app --port 8899 &
OLLAMA_API_KEY=test_key_1234567890 OLLAMA_BASE_URL=http://127.0.0.1:8899/v1 \
  uvicorn app:app --port 8800 &

python3 tests/test_backend.py    # API, стриминг, карта ошибок, отсутствие утечки ключа
python3 tests/test_frontend.py   # реальный Chromium: UI, Markdown, история, адаптив
python3 tests/test_extension.py  # расширение, загруженное в настоящий Chromium
```

Обе первые сюиты принимают `APP_URL`, поэтому ими же проверяется и Worker:

```bash
APP_URL=http://127.0.0.1:8790 python3 tests/test_backend.py
```

## Структура

```
app.py              backend: прокси к Ollama Cloud, SSE, обработка ошибок
models.json         каталог моделей + четыре карточки с запасными Model ID
static/index.html   разметка
static/styles.css   Pokémon-тема и ещё четыре стиля, адаптив
static/app.js       состояние, карточки моделей, Markdown-рендер, стриминг, UI
tests/              mock Ollama + проверки backend, браузера и расширения
Dockerfile          образ для Docker-хостинга (порт из $PORT, иначе 7860)
worker/worker_src.js тот же backend на JavaScript для Cloudflare Workers
worker/build_worker.py собирает worker.js: backend + вшитый фронтенд одним файлом
extension/         расширение Chrome: manifest, фоновый скрипт, иконки
extension/static/api.js  backend внутри страницы: перехватывает /api/* и ходит в Ollama
extension/build_extension.py собирает расширение из общих static/ и models.json
```
