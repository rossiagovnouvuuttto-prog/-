/**
 * Задачи для Ollama: промпты для модели и JSON-схемы ответов.
 *
 * Модуль не знает, как именно выполняется запрос — функция `chat` приходит
 * снаружи. Благодаря этому одна и та же логика работает и на сервере
 * (server/ollama.mjs), и прямо в браузере (ai/direct-provider.js).
 */

const ENGINEER = `You are a professional prompt engineer for AI image generators
(Midjourney, Stable Diffusion, Flux, DALL-E).

Rules:
- Always answer in English, regardless of the language of the request.
- Write comma-separated visual keywords, never full sentences or explanations.
- Be concrete and visual: describe what the camera would see.
- Never invent a different subject than the one the user described.
- Answer with JSON only, matching the requested schema.`;

/** Структура промпта, принятая на сайте. */
export const SECTIONS = ['subject', 'style', 'environment', 'camera', 'lighting', 'quality', 'detail', 'format'];

export const GENERATE_SCHEMA = {
  type: 'object',
  properties: {
    ...Object.fromEntries(SECTIONS.map((key) => [key, { type: 'string' }])),
    negative: { type: 'string' },
  },
  required: [...SECTIONS, 'negative'],
};

export const PING_SCHEMA = {
  type: 'object',
  properties: { ok: { type: 'boolean' } },
  required: ['ok'],
};

const TEXT_SCHEMA = {
  type: 'object',
  properties: { text: { type: 'string' } },
  required: ['text'],
};

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

/** Собирает ответ модели в промпт с той же разбивкой по строкам. */
function assemble(parts) {
  const lines = SECTIONS.map((key) => String(parts[key] || '').trim()).filter(Boolean);
  return { prompt: lines.join(', '), multiline: lines.join(',\n') };
}

/**
 * Создаёт набор задач поверх переданной функции запроса.
 * @param {(request: {system: string, user: string, schema: object}) => Promise<object>} chat
 */
export function createTasks(chat) {
  async function generate({ idea, settings }) {
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

  async function improve({ prompt, settings }) {
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

  async function variants({ idea, settings, count = 3 }) {
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

  async function translate({ text }) {
    const data = await chat({
      system: ENGINEER,
      user: `Translate this image prompt into English keywords. Keep every detail.

Text: "${text}"`,
      schema: TEXT_SCHEMA,
    });

    return { text: data.text };
  }

  async function translateRu({ text, idea }) {
    const data = await chat({
      system: `Ты переводишь промпты для генераторов изображений на русский язык.
Отвечай только JSON по схеме. Перевод должен быть понятным и естественным,
термины фотографии и рендера переводи принятыми в русском языке словами.`,
      user: `Перескажи по-русски, что описывает этот промпт. Сохрани разбивку по строкам.
${idea ? `Исходное описание пользователя: "${idea}" — используй его для первой строки.` : ''}

Промпт:
${text}`,
      schema: TEXT_SCHEMA,
    });

    return { text: data.text };
  }

  /** Короткая проверка связи. */
  async function ping() {
    const data = await chat({
      system: 'Reply with JSON only.',
      user: 'Return {"ok": true}.',
      schema: PING_SCHEMA,
    });
    return Boolean(data.ok);
  }

  return {
    generate,
    improve,
    variants,
    translate,
    translateRu,
    'translate-ru': translateRu,
    ping,
  };
}
