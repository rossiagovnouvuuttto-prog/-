# PromptAI

Генератор промптов для AI-изображений. Пользователь описывает идею обычными
словами — сервис превращает описание в профессиональный английский промпт для
Midjourney, Stable Diffusion, DALL·E, Flux и других генераторов.

Тёмная тема, градиенты, плавные анимации, mobile-first вёрстка.
Ни одной внешней зависимости: чистый HTML, CSS и ES-модули.

---

## Запуск

### Просто посмотреть сайт

Скачайте **`promptai.html`** и откройте его двойным кликом. Это автономная
сборка: весь CSS и JavaScript внутри одного файла, ничего устанавливать и
запускать не нужно, интернет не требуется.

### Для разработки

Исходники разбиты на ES-модули, поэтому `index.html` нужно открывать через
HTTP — с диска браузер модули заблокирует.

```bash
cd promptai

# любой из вариантов
python3 -m http.server 4173
npx http-server -p 4173 .
```

Затем откройте <http://localhost:4173>.

После изменений в `assets/` пересоберите автономную версию:

```bash
node build.mjs     # index.html + assets/ → promptai.html
```

Для публикации подойдёт любой статический хостинг: GitHub Pages, Netlify,
Vercel, Cloudflare Pages, обычный nginx.

---

## Структура

```
promptai/
├── promptai.html               # автономная сборка: один файл, открывается с диска
├── build.mjs                   # сборщик автономной версии
├── index.html                  # разметка страницы
├── manifest.webmanifest        # PWA-манифест (установка на телефон)
└── assets/
    ├── icon.svg
    ├── css/styles.css          # дизайн-система: токены, компоненты, брейкпоинты
    └── js/
        ├── app.js              # оркестрация: состояние + DOM
        ├── config.js           # лимиты, тарифы, настройки AI-провайдера
        ├── store.js            # localStorage: история, избранное, расход, аккаунты
        ├── auth.js             # система аккаунтов
        ├── quota.js            # бесплатный лимит генераций
        ├── utils.js
        ├── data/
        │   ├── styles.js       # 7 стилей с модификаторами и negative prompt
        │   ├── options.js      # камера, освещение, качество, формат
        │   └── dictionary.js   # русско-английский словарь
        ├── engine/
        │   ├── translator.js   # перевод описания в английские ключевые слова
        │   └── prompt-builder.js # сборка промпта по структуре
        ├── ai/
        │   ├── provider.js     # фасад: local или api
        │   ├── local-provider.js
        │   └── api-provider.js # готовый клиент для вашего бэкенда
        └── ui/
            ├── components.js   # шаблоны разметки
            ├── icons.js
            ├── modal.js
            └── toast.js
```

---

## Как собирается промпт

Промпт всегда строится по одной структуре — так работают профессиональные
промпт-инженеры:

```
Объект изображения + Стиль + Окружение + Камера + Освещение + Качество + Детализация
```

Пример для запроса «футуристический город с летающими машинами»
(стиль «Киберпанк», камера Cinematic, свет Neon lights, качество 8K, формат 16:9):

```
a futuristic city with flying cars,
cyberpunk style, futuristic dystopia,
rainy streets with neon lights,
cinematic camera angle, anamorphic wide shot,
neon lights, volumetric lighting, colored reflections,
8K detail, ultra high resolution,
ultra detailed, reflections and wet surfaces, chromatic aberration,
wide 16:9 composition
```

Дополнительно карточка результата отдаёт negative prompt стиля и параметр
соотношения сторон для Midjourney (`--ar 16:9`).

---

## Подключение настоящего AI

По умолчанию работает встроенный детерминированный движок (`provider: 'local'`) —
сайт полностью функционален без сети и без ключей.

Чтобы подключить реальную модель:

1. Поднимите бэкенд с одним POST-эндпоинтом.
2. В `assets/js/config.js` укажите:

```js
export const AI = {
  provider: 'api',            // вместо 'local'
  endpoint: '/api/prompt',    // адрес вашего эндпоинта
  timeoutMs: 20000,
  fallbackToLocal: true,      // если API упало — считаем локально
};
```

### Контракт эндпоинта

**Запрос**

```json
{
  "task": "generate",
  "idea": "футуристический город с летающими машинами",
  "settings": {
    "style": "cyberpunk",
    "camera": "cinematic",
    "lighting": "neon",
    "quality": "8k",
    "aspect": "16:9"
  }
}
```

`task` принимает значения `generate`, `improve`, `variants`, `translate`.
Для `improve` дополнительно приходит `prompt` и `multiline`, для `variants` — `count`,
для `translate` — `text`.

**Ответ**

```json
{
  "prompt": "a futuristic city with flying cars, cyberpunk style, ...",
  "multiline": "a futuristic city with flying cars,\ncyberpunk style, ...",
  "negative": "daylight, rustic, medieval, low contrast, watermark",
  "ratio": "--ar 16:9"
}
```

Для `variants` — `{ "variants": [{ "name": "Драматичный", "prompt": "..." }] }`,
для `translate` — `{ "text": "..." }`.

Ключи моделей держите на сервере: браузер обращается только к вашему эндпоинту.

---

## Тарифы и лимиты

Настраиваются в `assets/js/config.js` → `PLANS`:

| Тариф   | Генераций в сутки | Вариантов за раз |
|---------|-------------------|------------------|
| Гость   | 5                 | 2                |
| Free    | 20                | 3                |
| Premium | без ограничений   | 5                |

Расход считается отдельно для гостя и для каждого аккаунта, счётчик
обнуляется каждые сутки.

---

## Аккаунты

`auth.js` содержит демонстрационный провайдер: аккаунты лежат в localStorage,
пароль хранится как SHA-256 с солью. Этого достаточно для демонстрации
сценария, но **это не боевая авторизация** — она невозможна без сервера.

Чтобы подключить настоящую авторизацию, реализуйте те же методы
(`register`, `login`, `logout`, `current`, `setPlan`) поверх своего API и
подставьте объект в экспорт `auth`. Остальной код менять не придётся.

То же касается оплаты Premium: сейчас тариф переключается кнопкой, на проде
сюда приходит вебхук платёжной системы.

---

## Хранение данных

История (до 60 записей), избранное (до 120), настройки, аккаунты и счётчик
генераций хранятся в `localStorage` под префиксом `promptai:v1:`.
Если localStorage недоступен (приватный режим), данные живут в памяти вкладки.

---

## Поддержка браузеров

Chrome / Edge 90+, Safari 16+, Firefox 90+, мобильные Chrome и Safari.
Используются CSS-градиенты с маской, `backdrop-filter`, `overflow: clip`,
`IntersectionObserver` и ES-модули. Учитывается `prefers-reduced-motion`.
