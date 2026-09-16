---
title: Multi AI Chat
emoji: ⚡
colorFrom: yellow
colorTo: red
sdk: docker
app_port: 7860
pinned: false
short_description: Чат с нейросетями Hugging Face — DeepSeek, Qwen, Llama, Mistral, Gemma
---

# Multi AI Chat

Веб-чат с моделями Hugging Face: выбираете нейросеть и общаетесь. Яркий игровой
интерфейс в стиле Pokémon, потоковые ответы, Markdown с подсветкой кода, история
чатов, настройки генерации.

```
Браузер  ──POST /api/chat──▶  FastAPI (app.py)  ──Bearer HF_TOKEN──▶  Hugging Face Router
   ▲                                │
   └────────── SSE поток ───────────┘
```

Токен живёт только в процессе сервера. Фронтенд его не видит и не может увидеть:
браузер обращается только к `/api/chat`, заголовок `Authorization` добавляет backend.

## Возможности

- **Пять карточек на старте** — DeepSeek, Qwen, Llama, Mistral и DeepSeek API:
  иконка, название, краткое описание и живой статус Online / Offline.
  Один клик выбирает модель для чата
- **Два провайдера** — Hugging Face и собственный API DeepSeek. Модель с префиксом
  `deepseek:` уходит на api.deepseek.com со своим ключом, остальные — на HF
- **Выбор модели** — 24 модели в 8 категориях + любая своя по Hugging Face Model ID
- **Стриминг** — ответ печатается по мере генерации, есть кнопка «Остановить»
- **Markdown** — заголовки, списки, таблицы, цитаты, ссылки, блоки кода с подсветкой
  и кнопкой «Копировать код» (без внешних библиотек)
- **Под ответом** — Копировать · Повторить ответ · 👍 · 👎
- **История** — создать, открыть, переименовать, удалить чат, очистить всё (localStorage)
- **Настройки** — System Prompt, Temperature, Max Tokens, Top P
- **Адаптив** — телефон, планшет, ПК; на телефоне меню открывается через ☰
- **Ошибки** — неверный токен, недоступная модель, загрузка модели, rate limit,
  таймаут, обрыв сети: сайт продолжает работать и показывает уведомление
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

## Два провайдера

Маршрут задаёт сам Model ID, поэтому фронтенду о провайдерах знать не нужно:

```
deepseek-ai/DeepSeek-V3-0324   -> Hugging Face, ключ HF_TOKEN
deepseek:deepseek-chat         -> api.deepseek.com, ключ DEEPSEEK_API_KEY
```

Статусы карточек опрашиваются у каждого провайдера отдельно и своим ключом:
нет ключа — карточка офлайн, лишних запросов не делается. Ошибки тоже разные:
402 от DeepSeek читается как «пополните баланс», а не как код ошибки.

**API DeepSeek платный** — бесплатного лимита там нет, нужен положительный
баланс на platform.deepseek.com.

## Как выбирается рабочая модель

У каждой из четырёх карточек в `models.json` лежит не один Model ID, а список
`candidates` — модели одного семейства. При запросе `/api/featured` backend
спрашивает у роутера Hugging Face список обслуживаемых моделей и берёт **первый
кандидат, который реально работает**:

```
DeepSeek : DeepSeek-V3-0324 → DeepSeek-V3 → DeepSeek-R1-Distill-Qwen-32B
Qwen     : Qwen2.5-72B-Instruct → Qwen2.5-7B-Instruct → Qwen3-235B-A22B-Instruct-2507
Llama    : Llama-3.3-70B-Instruct → Llama-3.1-8B-Instruct → Meta-Llama-3-8B-Instruct
Mistral  : Mistral-7B-Instruct-v0.3 → Mistral-Small-24B-Instruct-2501 → Mixtral-8x7B
```

Если ни один кандидат семейства не обслуживается, карточка честно показывает
**Offline**, а попытка написать в неё выдаёт обычное сообщение об ошибке — сайт
не ломается. Результат кешируется на 5 минут; `/api/featured?refresh=true`
пересчитывает его сразу. Когда список моделей недоступен, backend переходит на
запасной путь и проверяет кандидатов пробным запросом.

## Где хранится HF_TOKEN

В переменной окружения `HF_TOKEN` на сервере — и только там.

| Площадка | Куда добавить |
| --- | --- |
| Hugging Face Space | Settings → Variables and secrets → **New secret** → `HF_TOKEN` |
| GitHub Actions (автодеплой) | Settings → Secrets and variables → Actions → `HF_TOKEN` |
| Cloudflare Worker | Settings → Variables and Secrets → тип **Secret** → `HF_TOKEN` |
| Расширение Chrome | Настройки в самом чате → поля «Токен Hugging Face» и «Ключ DeepSeek» (хранилище браузера) |
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

**Способ 3 — новая карточка.** Добавьте элемент в массив `featured`, указав `name`,
`icon`, `desc`, `type` (`electric` / `fire` / `water` / `grass` — задаёт цвет карточки)
и список `candidates` с Model ID одного семейства.

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

## Расширение для Chrome (без хостинга вообще)

Третья сборка: тот же чат как расширение браузера. Ни аккаунтов, ни деплоя —
работает локально, токен хранится в хранилище расширения.

1. Распакуйте `multi-ai-chat-extension.zip`.
2. Откройте `chrome://extensions`, включите **Режим разработчика** справа вверху.
3. **Загрузить распакованное расширение** → выберите распакованную папку.
4. Нажмите на иконку покебола на панели → откроется чат.
5. **Настройки** → поле **Токен Hugging Face** → вставьте токен.

`static/app.js` и `static/styles.css` копируются из `static/` без единого
изменения, поэтому интерфейс тот же. Роль backend играет `static/api.js`: он
перехватывает те же запросы `/api/*` прямо в странице и сам ходит в Hugging Face.
`app.js` по-прежнему вызывает `fetch('/api/chat')` и токена не видит.

Пересборка после правок в `static/`:

```bash
python3 extension/build_extension.py
```

## Публикация на Cloudflare Workers (без Git и без оплаты)

Docker-хостинг на Hugging Face стал платным, поэтому есть вторая сборка: тот же
сайт целиком (фронтенд + backend) упакован в один файл `worker/worker.js`, который
вставляется прямо в браузере.

1. Зарегистрируйтесь на [cloudflare.com](https://dash.cloudflare.com/sign-up).
2. **Compute (Workers)** → **Create** → **Start from Hello World** → задайте имя
   `multi-ai-chat` → **Deploy**.
3. Откройте **Edit code**, выделите весь пример и вставьте содержимое `worker/worker.js`
   → **Deploy**.
4. **Settings** → **Variables and Secrets** → **Add** → тип **Secret**,
   имя `HF_TOKEN`, значение — токен с huggingface.co → **Deploy**.

Сайт открывается по адресу `https://multi-ai-chat.<ваш-поддомен>.workers.dev`.

Токен и здесь остаётся только на сервере: он хранится в секретах Worker, а браузер
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
| `GET` | `/api/health` | Статус сервера и факт наличия токена |
| `GET` | `/api/models` | Каталог моделей из `models.json` |
| `GET` | `/api/featured` | Четыре карточки с подобранным Model ID и статусом |
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
| `DEEPSEEK_API_KEY` | — | Ключ DeepSeek. Без него карточка «DeepSeek API» офлайн |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com/v1` | Точка входа DeepSeek |

## Тесты

`tests/mock_hf.py` подменяет Hugging Face и умеет воспроизводить каждую ошибку,
поэтому все ветки проверяются без обращения к сети.

```bash
uvicorn tests.mock_hf:app --port 8899 &
HF_TOKEN=hf_fake HF_BASE_URL=http://127.0.0.1:8899/v1 uvicorn app:app --port 8800 &

python3 tests/test_backend.py    # API, стриминг, карта ошибок, отсутствие утечки токена
python3 tests/test_frontend.py   # реальный Chromium: UI, Markdown, история, адаптив
python3 tests/test_extension.py  # расширение, загруженное в настоящий Chromium
```

Обе первые сюиты принимают `APP_URL`, поэтому ими же проверяется и Worker:

```bash
APP_URL=http://127.0.0.1:8790 python3 tests/test_backend.py
```

## Структура

```
app.py              backend: прокси к Hugging Face, SSE, обработка ошибок
models.json         каталог моделей + четыре карточки с запасными Model ID
static/index.html   разметка
static/styles.css   Pokémon-тема, адаптив
static/app.js       состояние, карточки моделей, Markdown-рендер, стриминг, UI
tests/              mock Hugging Face + проверки backend и браузера
Dockerfile          образ для Docker-хостинга (порт из $PORT, иначе 7860)
worker/worker_src.js тот же backend на JavaScript для Cloudflare Workers
worker/build_worker.py собирает worker.js: backend + вшитый фронтенд одним файлом
extension/         расширение Chrome: manifest, фоновый скрипт, иконки
extension/static/api.js  backend внутри страницы: перехватывает /api/* и ходит в HF
extension/build_extension.py собирает расширение из общих static/ и models.json
```
