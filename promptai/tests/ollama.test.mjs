/**
 * Проверяет, что сервер обращается к Ollama правильно: нужный адрес,
 * заголовок с ключом и разбор ответа. Настоящий ключ не нужен —
 * сетевой вызов подменяется.
 *
 * Запуск: node tests/ollama.test.mjs
 */

let failed = 0;
const check = (name, ok, extra = '') => {
  if (!ok) failed += 1;
  console.log(`${ok ? '✓' : '✗'} ${name}${extra ? ` — ${extra}` : ''}`);
};

/** Подменяет fetch и возвращает перехваченный запрос. */
function stubFetch(answer) {
  const seen = {};
  globalThis.fetch = async (url, options) => {
    seen.url = String(url);
    seen.headers = options.headers;
    seen.body = JSON.parse(options.body);
    return {
      ok: true,
      status: 200,
      text: async () => JSON.stringify({ message: { content: JSON.stringify(answer) } }),
    };
  };
  return seen;
}

/* — облако: ключ задан, свой адрес не указан — */
process.env.OLLAMA_API_KEY = 'test-key-123';
delete process.env.OLLAMA_HOST;
delete process.env.OLLAMA_MODEL;

const cloud = await import('../server/ollama.mjs');
let seen = stubFetch({
  subject: 'a cat', style: 'anime style', environment: 'city', camera: 'macro shot',
  lighting: 'neon lights', quality: '8K detail', detail: 'ultra detailed',
  format: 'wide 16:9 composition', negative: 'watermark',
});

const result = await cloud.generate({ idea: 'кот', settings: { style: 'anime' } });

check('адрес облака', seen.url === 'https://ollama.com/api/chat', seen.url);
check('ключ уходит в заголовке', seen.headers.Authorization === 'Bearer test-key-123');
check('запрос не потоковый', seen.body.stream === false);
check('задана JSON-схема', Boolean(seen.body.format?.properties?.subject));
check('модель по умолчанию для облака', seen.body.model === 'gpt-oss:120b-cloud', seen.body.model);
check('промпт собран по структуре', result.multiline.split('\n').length === 8, `${result.multiline.split('\n').length} строк`);
check('negative prompt передан', result.negative === 'watermark');
check('описание пользователя дошло до модели', seen.body.messages[1].content.includes('кот'));

/* — свой адрес: ключ всё равно нужен, если задан — */
seen = stubFetch({ text: 'a cat' });
cloud.config.host = 'http://my-proxy:1234';
await cloud.translate({ text: 'кот' });
check('свой адрес учитывается', seen.url === 'http://my-proxy:1234/api/chat', seen.url);
check('ключ уходит и на свой адрес', seen.headers.Authorization === 'Bearer test-key-123');

/* — локальная Ollama: ключа нет — */
seen = stubFetch({ text: 'a cat' });
cloud.config.apiKey = '';
cloud.config.host = '';
cloud.config.model = '';
await cloud.translate({ text: 'кот' });
check('локальный адрес без ключа', seen.url === 'http://localhost:11434/api/chat', seen.url);
check('без ключа заголовка нет', !('Authorization' in seen.headers));
check('модель по умолчанию для локальной', seen.body.model === 'llama3.2', seen.body.model);

/* — ошибка Ollama доходит до вызывающего — */
globalThis.fetch = async () => ({
  ok: false,
  status: 404,
  text: async () => JSON.stringify({ error: 'model "nope" not found' }),
});

try {
  await cloud.translate({ text: 'кот' });
  check('ошибка Ollama пробрасывается', false, 'исключения не было');
} catch (error) {
  check('ошибка Ollama пробрасывается с текстом', error.message.includes('not found'), error.message);
}

console.log(failed ? `\n${failed} проверок провалено` : '\nвсе проверки пройдены');
process.exit(failed ? 1 : 0);
