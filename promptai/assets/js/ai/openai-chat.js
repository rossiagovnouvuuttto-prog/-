/**
 * Запрос к сервису с OpenAI-совместимым API (GLM, DeepSeek, OpenAI и другие).
 *
 * Схема ответа передаётся модели текстом: в этом формате нет отдельного
 * поля для JSON-схемы, зато есть режим response_format: json_object.
 * Ответ на всякий случай очищается от ```-обёрток — некоторые модели
 * добавляют их даже в режиме JSON.
 */

/** Достаёт JSON даже если модель обернула его в текст или ```json. */
export function parseJson(content) {
  const cleaned = String(content)
    .replace(/^\s*```(?:json)?/i, '')
    .replace(/```\s*$/, '')
    .trim();

  try {
    return JSON.parse(cleaned);
  } catch {
    // Последняя попытка: вырезаем самый внешний объект.
    const start = cleaned.indexOf('{');
    const end = cleaned.lastIndexOf('}');
    if (start === -1 || end <= start) throw new Error('Модель ответила не в формате JSON');
    return JSON.parse(cleaned.slice(start, end + 1));
  }
}

/**
 * @param {object} options
 * @param {() => {host: string, model: string, apiKey: string}} options.settings
 * @param {(url: string, init: object) => Promise<Response>} [options.request] — для тестов
 * @param {number} [options.timeoutMs]
 */
export function createOpenAiChat({ settings, request = fetch, timeoutMs = 90000 }) {
  return async function chat({ system, user, schema }) {
    const { host, model, apiKey } = settings();

    const instruction = `${system}

Answer with a single JSON object matching exactly this schema, with no extra keys and no markdown:
${JSON.stringify(schema)}`;

    const response = await request(`${host.replace(/\/+$/, '')}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(apiKey ? { Authorization: `Bearer ${apiKey}` } : {}),
      },
      body: JSON.stringify({
        model,
        temperature: 0.8,
        response_format: { type: 'json_object' },
        messages: [
          { role: 'system', content: instruction },
          { role: 'user', content: user },
        ],
      }),
      signal: AbortSignal.timeout(timeoutMs),
    });

    const raw = await response.text();

    if (!response.ok) {
      let detail = raw.slice(0, 300);
      try {
        const parsed = JSON.parse(raw);
        detail = parsed.error?.message || parsed.error || parsed.message || detail;
      } catch { /* не JSON */ }
      throw new Error(`${response.status}: ${detail}`);
    }

    const content = JSON.parse(raw)?.choices?.[0]?.message?.content;
    if (!content) throw new Error('Сервис вернул пустой ответ');

    return parseJson(content);
  };
}
