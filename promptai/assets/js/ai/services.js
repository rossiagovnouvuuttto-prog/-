/**
 * Сервисы, к которым умеет подключаться сайт.
 *
 * `api` определяет формат запроса:
 *   'ollama' — POST {host}/api/chat, ответ в message.content
 *   'openai' — POST {host}/chat/completions, ответ в choices[0].message.content
 *
 * OpenAI-совместимый формат понимают очень многие сервисы, поэтому кроме
 * перечисленных подойдёт любой — достаточно указать его адрес вручную.
 */

export const SERVICES = {
  glm: {
    label: 'GLM / Zhipu',
    api: 'openai',
    host: 'https://open.bigmodel.cn/api/paas/v4',
    model: 'glm-4.7-flash',
    needsKey: true,
    keyHint: 'Ключ из личного кабинета bigmodel.cn',
    modelHint: 'glm-4.7-flash — бесплатная',
  },
  'ollama-cloud': {
    label: 'Ollama — облако',
    api: 'ollama',
    host: 'https://ollama.com',
    model: 'gpt-oss:120b-cloud',
    needsKey: true,
    keyHint: 'Ключ с ollama.com/settings/keys',
    modelHint: 'список моделей: ollama.com/search?c=cloud',
  },
  'ollama-local': {
    label: 'Ollama на этом компьютере',
    api: 'ollama',
    host: 'http://localhost:11434',
    model: 'llama3.2',
    needsKey: false,
    keyHint: '',
    modelHint: 'модель, скачанная командой ollama pull',
  },
  openai: {
    label: 'Другой OpenAI-совместимый',
    api: 'openai',
    host: '',
    model: '',
    needsKey: true,
    keyHint: 'Ключ вашего сервиса',
    modelHint: 'название модели у вашего сервиса',
  },
};

export const DEFAULT_SERVICE = 'glm';

export const getService = (id) => SERVICES[id] || SERVICES[DEFAULT_SERVICE];
