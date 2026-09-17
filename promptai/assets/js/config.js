/**
 * Глобальная конфигурация PromptAI.
 * Единственное место, где меняются лимиты, тарифы и параметры AI-провайдера.
 */

export const APP = {
  name: 'PromptAI',
  version: '1.0.0',
  storagePrefix: 'promptai:v1:',
};

/**
 * Настройки AI-провайдера.
 *
 * provider: 'local' — встроенный детерминированный движок (работает без сети).
 *           'api'   — запросы уходят на ваш бэкенд (см. assets/js/ai/api-provider.js).
 *
 * Чтобы подключить реальный AI, достаточно поднять бэкенд с одним POST-эндпоинтом
 * и переключить provider на 'api'. Ключи моделей держите на сервере, не в браузере.
 */
export const AI = {
  provider: 'local',
  endpoint: '/api/prompt',
  timeoutMs: 20000,
  /** Куда падать, если удалённый провайдер недоступен. */
  fallbackToLocal: true,
  /** Искусственная задержка локального движка — чтобы UI ощущался «живым». */
  localLatencyMs: [420, 900],
};

/** Тарифы и лимиты генераций. */
export const PLANS = {
  guest: {
    id: 'guest',
    label: 'Гость',
    dailyLimit: 5,
    variants: 2,
    features: ['Базовая генерация', 'История на устройстве'],
  },
  free: {
    id: 'free',
    label: 'Free',
    price: '0 ₽',
    period: 'навсегда',
    desc: 'Чтобы попробовать и собрать первые промпты.',
    dailyLimit: 20,
    variants: 3,
    features: [
      '20 генераций в день',
      'Все 7 стилей и настройки',
      'Улучшение промпта',
      'История и избранное',
      'Перевод описания на английский',
    ],
  },
  premium: {
    id: 'premium',
    label: 'Premium',
    price: '399 ₽',
    period: 'в месяц',
    desc: 'Для тех, кто генерирует каждый день.',
    dailyLimit: Infinity,
    variants: 5,
    features: [
      'Безлимитные генерации',
      'До 5 вариантов за раз',
      'Расширенное улучшение промпта',
      'Negative prompt и параметры моделей',
      'Безлимитная история и избранное',
      'Ранний доступ к новым стилям',
    ],
  },
};

/** Значения настроек по умолчанию. */
export const DEFAULT_SETTINGS = {
  style: 'realism',
  camera: 'dslr',
  lighting: 'studio',
  quality: '4k',
  aspect: '16:9',
};

/** Ограничения интерфейса. */
export const LIMITS = {
  ideaMaxLength: 600,
  historySize: 60,
  favoritesSize: 120,
};
