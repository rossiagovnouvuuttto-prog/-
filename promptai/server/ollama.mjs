/**
 * Клиент Ollama и промпты для модели.
 *
 * Ключ живёт только здесь, на сервере. Браузер к Ollama не обращается —
 * он стучится в наш /api/prompt, поэтому ключ не попадает на страницу.
 *
 * Поддерживаются оба режима:
 *   • облако  — https://ollama.com/api/chat с заголовком Authorization: Bearer
 *   • локально — http://localhost:11434/api/chat, ключ не нужен
 */

const CLOUD_HOST = 'https://ollama.com';
const LOCAL_HOST = 'http://localhost:11434';

export const config = {
  apiKey: process.env.OLLAMA_API_KEY || '',
  host: process.env.OLLAMA_HOST || '',
  model: process.env.OLLAMA_MODEL || '',
  timeoutMs: Number(process.env.OLLAMA_TIMEOUT_MS || 60000),
};

/** Ключ есть — значит, работаем с облаком (или с прокси к нему). */
export const isCloud = () => Boolean(config.apiKey);

export const host = () => config.host || (isCloud() ? CLOUD_HOST : LOCAL_HOST);

export const model = () => config.model || (isCloud() ? 'gpt-oss:120b-cloud' : 'llama3.2');

/** Запрос к Ollama с ответом строго по JSON-схеме. */
async function chat({ system, user, schema }) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.timeoutMs);

  try {
    const response = await fetch(`${host()}/api/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // Ключ уходит только отсюда, с сервера, и только в Ollama.
        ...(config.apiKey ? { Authorization: `Bearer ${config.apiKey}` } : {}),
      },
      body: JSON.stringify({
        model: model(),
        stream: false,
        format: schema,
        options: { temperature: 0.8 },
        messages: [
          { role: 'system', content: system },
          { role: 'user', content: user },
        ],
      }),
      signal: controller.signal,
    });

    const raw = await response.text();

    if (!response.ok) {
      // Ollama возвращает понятный текст ошибки — отдаём его как есть.
      let detail = raw.slice(0, 300);
      try {
        detail = JSON.parse(raw).error || detail;
      } catch { /* не JSON — оставляем как есть */ }
      throw new Error(`Ollama ${response.status}: ${detail}`);
    }

    const content = JSON.parse(raw)?.message?.content;
    if (!content) throw new Error('Ollama вернул пустой ответ');

    return JSON.parse(content);
  } finally {
    clearTimeout(timer);
  }
}

/* ────────────────────────  промпты для модели  ──────────────────────── */

const ENGINEER = `You are a professional prompt engineer for AI image generators
(Midjourney, Stable Diffusion, Flux, DALL-E).

Rules:
- Always answer in English, regardless of the language of the request.
- Write comma-separated visual keywords, never full sentences or explanations.
- Be concrete and visual: describe what the camera would see.
- Never invent a different subject than the one the user described.
- Answer with JSON only, matching the requested schema.`;

/** Описание выбранных настроек — чтобы модель их учитывала. */
function describe(settings = {}) {
  return [
    settings.style && `style: ${settings.style}`,
    settings.camera && `camera: ${settings.camera}`,
    settings.lighting && `lighting: ${settings.lighting}`,
    settings.quality && `quality: ${settings.quality}`,
    settings.aspect && `aspect ratio: ${settings.aspect}`,
  ].filter(Boolean).join(', ');
}

/**
 * Схема повторяет структуру промпта, принятую на сайте, поэтому результат
 * модели раскладывается по тем же строкам, что и у локального движка.
 */
const SECTIONS = ['subject', 'style', 'environment', 'camera', 'lighting', 'quality', 'detail', 'format'];

const GENERATE_SCHEMA = {
  type: 'object',
  properties: {
    ...Object.fromEntries(SECTIONS.map((key) => [key, { type: 'string' }])),
    negative: { type: 'string' },
  },
  required: [...SECTIONS, 'negative'],
};

const assemble = (parts) => {
  const lines = SECTIONS.map((key) => String(parts[key] || '').trim()).filter(Boolean);
  return { prompt: lines.join(', '), multiline: lines.join(',\n') };
};

export async function generate({ idea, settings }) {
  const parts = await chat({
    system: ENGINEER,
    user: `Build an image prompt from this description: "${idea}"

Selected settings — ${describe(settings)}.

Fill every field with comma-separated English keywords:
- subject: what is depicted, translated from the description
- style: the artistic style
- environment: surroundings and atmosphere
- camera: shot type, lens, angle
- lighting: light sources and mood
- quality: resolution and sharpness
- detail: level of detail and finishing touches
- format: composition for the aspect ratio
- negative: what the image must NOT contain`,
    schema: GENERATE_SCHEMA,
  });

  return { ...assemble(parts), negative: parts.negative || '' };
}

export async function improve({ prompt, settings }) {
  const parts = await chat({
    system: ENGINEER,
    user: `Improve this image prompt without changing its subject.
Add composition, color grading, atmosphere and a finishing touch.

Prompt: "${prompt}"
Settings — ${describe(settings)}.

Return the improved prompt in the same fields.`,
    schema: GENERATE_SCHEMA,
  });

  return { ...assemble(parts), negative: parts.negative || '' };
}

export async function variants({ idea, settings, count = 3 }) {
  const data = await chat({
    system: ENGINEER,
    user: `Create ${count} different image prompts for the same description: "${idea}".
Settings — ${describe(settings)}.

Keep the subject identical in every variant; change mood, lighting, composition
and environment. Give each variant a short Russian name (1-2 words).`,
    schema: {
      type: 'object',
      properties: {
        variants: {
          type: 'array',
          items: {
            type: 'object',
            properties: { name: { type: 'string' }, prompt: { type: 'string' } },
            required: ['name', 'prompt'],
          },
        },
      },
      required: ['variants'],
    },
  });

  return {
    variants: (data.variants || []).slice(0, count).map((variant, index) => ({
      id: `ai-${index}`,
      name: variant.name || `Вариант ${index + 1}`,
      prompt: variant.prompt,
      multiline: String(variant.prompt).split(/,\s*/).join(',\n'),
    })),
  };
}

export async function translate({ text }) {
  const data = await chat({
    system: ENGINEER,
    user: `Translate this image prompt into English keywords. Keep every detail.

Text: "${text}"`,
    schema: { type: 'object', properties: { text: { type: 'string' } }, required: ['text'] },
  });

  return { text: data.text };
}

export async function translateRu({ text, idea }) {
  const data = await chat({
    system: `Ты переводишь промпты для генераторов изображений на русский язык.
Отвечай только JSON по схеме. Перевод должен быть понятным и естественным,
термины фотографии и рендера переводи принятыми в русском языке словами.`,
    user: `Перескажи по-русски, что описывает этот промпт. Сохрани разбивку по строкам.
${idea ? `Исходное описание пользователя: "${idea}" — используй его для первой строки.` : ''}

Промпт:
${text}`,
    schema: { type: 'object', properties: { text: { type: 'string' } }, required: ['text'] },
  });

  return { text: data.text };
}

/** Короткая проверка связи — вызывается при старте сервера. */
export async function ping() {
  const data = await chat({
    system: 'Reply with JSON only.',
    user: 'Return {"ok": true}.',
    schema: { type: 'object', properties: { ok: { type: 'boolean' } }, required: ['ok'] },
  });
  return Boolean(data.ok);
}

export const TASKS = { generate, improve, variants, translate, 'translate-ru': translateRu };
