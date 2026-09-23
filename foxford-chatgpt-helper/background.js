/**
 * Service worker.
 * Никаких сетевых запросов, API-ключей, cookies или токенов: он открывает вкладку
 * https://chatgpt.com/ (в уже авторизованной сессии пользователя), передаёт туда
 * промпт через chrome.storage.local и показывает уведомление. Вход в аккаунт не выполняется.
 */

const CHATGPT_URL = 'https://chatgpt.com/';
const NOTICE_TEXT = 'Задание скопировано — вставьте его в ChatGPT';

const DEFAULTS = {
  theme: 'auto',
  detail: 'normal',
  extraInstruction: '',
  systemNotification: true,
  autoSend: true
};

async function openChatGpt(sourceTab, prompt) {
  // Одноразовый промпт кладём в хранилище ДО открытия вкладки: content-script
  // на chatgpt.com заберёт его и вставит в поле чата.
  if (prompt) {
    await chrome.storage.local.set({ pendingPrompt: { text: prompt, ts: Date.now() } });
  }
  const createProps = { url: CHATGPT_URL, active: true };
  if (sourceTab && typeof sourceTab.index === 'number') {
    createProps.index = sourceTab.index + 1;
    createProps.windowId = sourceTab.windowId;
  }
  return chrome.tabs.create(createProps);
}

async function notify(message) {
  const { systemNotification } = await chrome.storage.local.get({ systemNotification: DEFAULTS.systemNotification });
  if (!systemNotification) return;
  try {
    await chrome.notifications.create('fxg-copied-' + Date.now(), {
      type: 'basic',
      iconUrl: chrome.runtime.getURL('icons/icon128.png'),
      title: 'Foxford → ChatGPT',
      message: message || NOTICE_TEXT,
      priority: 1
    });
  } catch (e) {
    // уведомления могут быть отключены в ОС — это не критично
  }
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  (async () => {
    try {
      switch (message?.type) {
        case 'OPEN_CHATGPT':
          await openChatGpt(sender.tab || message.sourceTab, message.prompt);
          if (message.notify !== false) await notify(message.prompt ? 'Открываю ChatGPT и вставляю задание…' : NOTICE_TEXT);
          sendResponse({ ok: true });
          break;
        case 'NOTIFY':
          await notify(message.text);
          sendResponse({ ok: true });
          break;
        case 'OPEN_OPTIONS':
          await chrome.runtime.openOptionsPage();
          sendResponse({ ok: true });
          break;
        default:
          sendResponse({ ok: false, message: 'Неизвестный запрос.' });
      }
    } catch (e) {
      sendResponse({ ok: false, message: e?.message || String(e) });
    }
  })();
  return true; // ответ асинхронный
});

chrome.runtime.onInstalled.addListener(async (details) => {
  // Убираем данные прошлой версии, которая работала через API (ключ, модель и т. п.).
  await chrome.storage.local.remove(['apiKey', 'model', 'maxTokens', 'enabled', 'pendingPrompt']);
  const current = await chrome.storage.local.get(DEFAULTS);
  await chrome.storage.local.set({ ...DEFAULTS, ...current });
  if (details.reason === 'install') chrome.runtime.openOptionsPage();
});
