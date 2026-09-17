/**
 * Провайдер на основе HTTP API.
 *
 * Ожидаемый контракт бэкенда — один POST-эндпоинт (AI.endpoint):
 *
 *   POST /api/prompt
 *   {
 *     "task": "generate" | "improve" | "variants" | "translate" | "translate-ru",
 *     "idea": "футуристический город",        // для generate/variants
 *     "prompt": "a futuristic city, ...",     // для improve
 *     "text": "текст",                        // для translate
 *     "settings": { "style": "cyberpunk", "camera": "cinematic",
 *                   "lighting": "neon", "quality": "8k", "aspect": "16:9" },
 *     "count": 3                              // для variants
 *   }
 *
 *   200 OK
 *   { "prompt": "...", "multiline": "...", "negative": "...", "ratio": "--ar 16:9" }
 *   { "variants": [{ "name": "...", "prompt": "..." }] }
 *   { "text": "..." }
 *
 * Ключи моделей должны оставаться на сервере: браузер к ним не обращается.
 */

import { AI } from '../config.js';

async function request(task, payload) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), AI.timeoutMs);

  try {
    const response = await fetch(AI.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ task, ...payload }),
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(`AI API вернул ${response.status}`);
    }

    return await response.json();
  } finally {
    clearTimeout(timer);
  }
}

export const apiProvider = {
  id: 'api',

  async generate({ idea, settings }) {
    const data = await request('generate', { idea, settings });
    return {
      prompt: data.prompt,
      multiline: data.multiline || data.prompt,
      parts: data.parts || {},
      negative: data.negative || '',
      ratio: data.ratio || '',
      source: 'api',
    };
  },

  async improve({ prompt, multiline, settings }) {
    const data = await request('improve', { prompt, multiline, settings });
    return {
      prompt: data.prompt,
      multiline: data.multiline || data.prompt,
      added: data.added || [],
      source: 'api',
    };
  },

  async variants({ idea, settings, count }) {
    const data = await request('variants', { idea, settings, count });
    return {
      variants: (data.variants || []).map((item, index) => ({
        id: item.id || `api-${index}`,
        name: item.name || `Вариант ${index + 1}`,
        prompt: item.prompt,
        multiline: item.multiline || item.prompt,
      })),
      source: 'api',
    };
  },

  async translate({ text }) {
    const data = await request('translate', { text });
    return { text: data.text, source: 'api' };
  },

  async translateRu({ text, idea }) {
    const data = await request('translate-ru', { text, idea });
    return { text: data.text, source: 'api' };
  },

  async translateIdea({ idea }) {
    const data = await request('translate', { text: idea });
    return { text: data.text, source: 'api' };
  },
};
