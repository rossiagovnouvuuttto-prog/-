/**
 * PromptAI — точка входа.
 *
 * Модуль связывает состояние (store), AI-провайдер и интерфейс.
 * Вся бизнес-логика вынесена в engine/ и ai/, здесь остаётся только
 * оркестрация и работа с DOM.
 */

import { ai, resetProvider } from './ai/provider.js';
import { clearConnection, readConnection, saveConnection } from './ai/connection.js';
import { directProvider, explain } from './ai/direct-provider.js';
import { auth, currentPlan } from './auth.js';
import { LIMITS, PLANS } from './config.js';
import { GROUP_MAP, IDEA_SEEDS, getOption } from './data/options.js';
import { getStyle } from './data/styles.js';
import { maxVariants, quota, spendGeneration } from './quota.js';
import { store } from './store.js';
import {
  emptyState, faqItems, featureCards, libraryEntry, pricingCards,
  resultCard, settingGroups, skeleton, styleCards, translationCard, variantCard,
} from './ui/components.js';
import { icon } from './ui/icons.js';
import { closeModal, openModal } from './ui/modal.js';
import { toast } from './ui/toast.js';
import { $, $$, copyText, debounce, escapeHtml, haptic, hasCyrillic, pick, plural, uid } from './utils.js';

/* ──────────────────────────  состояние сессии  ────────────────────────── */

const state = {
  settings: store.getSettings(),
  entry: null,        // текущий показанный промпт
  tab: 'history',
  busy: false,
};

/* ──────────────────────────  вспомогательные  ────────────────────────── */

/** Человекочитаемые подписи выбранных настроек — для карточки результата. */
function settingLabels(settings) {
  return Object.fromEntries(
    Object.keys(GROUP_MAP).map((groupId) => [groupId, getOption(groupId, settings[groupId])?.label]),
  );
}

function setLoading(button, loading) {
  if (!button) return;
  button.classList.toggle('is-loading', loading);
  button.disabled = loading;
}

/* ──────────────────────────  генерация  ────────────────────────── */

async function generate(ideaOverride) {
  if (state.busy) return;

  const input = $('#ideaInput');
  const idea = (ideaOverride ?? input.value).trim();

  if (idea.length < 2) {
    toast('Опишите изображение хотя бы парой слов', 'error');
    input.focus();
    return;
  }

  const limits = quota();
  if (limits.exhausted) {
    if ($('#modalRoot').hidden) showPaywall(limits);
    return;
  }

  state.busy = true;
  const button = $('#generateBtn');
  setLoading(button, true);
  $('#resultZone').innerHTML = skeleton();

  try {
    const result = await ai.generate({ idea, settings: state.settings });
    spendGeneration();

    const style = getStyle(state.settings.style);
    const entry = store.addHistory({
      id: uid(),
      idea,
      prompt: result.prompt,
      multiline: result.multiline,
      negative: result.negative,
      ratio: result.ratio,
      degraded: result.degraded,
      styleId: style.id,
      styleName: style.name,
      settings: { ...state.settings },
      settingLabels: settingLabels(state.settings),
    });

    state.entry = entry;
    renderResult();
    haptic();

    const left = quota();
    if (!left.unlimited && left.left <= 2) {
      toast(`Осталось генераций сегодня: ${left.left}`, 'info', 3200);
    }
  } catch (error) {
    console.error(error);
    $('#resultZone').innerHTML = emptyState(
      'Не удалось создать промпт',
      'Проверьте подключение и попробуйте ещё раз.',
      '⚠️',
    );
    toast('Ошибка генерации. Попробуйте ещё раз', 'error');
  } finally {
    state.busy = false;
    setLoading(button, false);
    renderQuota();
  }
}

function renderResult() {
  const zone = $('#resultZone');
  if (!state.entry) {
    zone.innerHTML = '';
    return;
  }

  zone.innerHTML = resultCard(state.entry, { isFavorite: store.isFavorite(state.entry.id) });
  zone.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

/* ──────────────────────────  действия над промптом  ────────────────────────── */

async function improvePrompt(button) {
  if (!state.entry) return;
  setLoading(button, true);

  try {
    const result = await ai.improve({
      prompt: state.entry.prompt,
      multiline: state.entry.multiline,
      settings: state.entry.settings,
    });
    state.entry = { ...state.entry, prompt: result.prompt, multiline: result.multiline };
    store.updateHistory(state.entry.id, { prompt: result.prompt, multiline: result.multiline });
    renderResult();
    toast('Промпт улучшен: добавлены композиция, цвет и атмосфера', 'success');
  } catch {
    toast('Не удалось улучшить промпт', 'error');
  } finally {
    setLoading(button, false);
  }
}

async function makeVariants(button) {
  if (!state.entry) return;
  setLoading(button, true);

  try {
    const count = maxVariants();
    const { variants } = await ai.variants({
      idea: state.entry.idea,
      settings: state.entry.settings,
      count,
    });

    const list = $('#variantList');
    list.innerHTML = variants.map(variantCard).join('');

    const plan = currentPlan();
    if (plan.id !== PLANS.premium.id) {
      list.insertAdjacentHTML('beforeend', `
        <p class="composer__hint">
          Доступно ${count} ${plural(count, ['вариант', 'варианта', 'вариантов'])} на тарифе ${plan.label}.
          <button class="link-btn" type="button" data-action="open-account">Premium — до 5 вариантов</button>
        </p>`);
    }
    toast(`Готово: ${variants.length} ${plural(variants.length, ['вариант', 'варианта', 'вариантов'])} промпта`, 'success');
  } catch {
    toast('Не удалось создать варианты', 'error');
  } finally {
    setLoading(button, false);
  }
}

async function translateCurrent(button) {
  if (!state.entry) return;
  setLoading(button, true);

  try {
    const { text } = await ai.translate({ text: state.entry.prompt });

    if (!hasCyrillic(state.entry.prompt) && text === state.entry.prompt) {
      toast('Промпт уже полностью на английском', 'info');
      return;
    }

    state.entry = { ...state.entry, prompt: text, multiline: text.split(/,\s*/).join(',\n') };
    store.updateHistory(state.entry.id, { prompt: state.entry.prompt, multiline: state.entry.multiline });
    renderResult();
    toast('Промпт переведён на английский', 'success');
  } catch {
    toast('Не удалось перевести промпт', 'error');
  } finally {
    setLoading(button, false);
  }
}

/**
 * Показывает русский перевод промпта под карточкой.
 * Сам промпт не меняется: копируется и уходит в генератор английский текст.
 */
async function showRussian(button) {
  if (!state.entry) return;

  const box = $('#translationBox');
  if (!box) return;

  // Повторное нажатие сворачивает перевод.
  if (!box.hidden) {
    box.hidden = true;
    box.innerHTML = '';
    return;
  }

  setLoading(button, true);

  try {
    const { text } = await ai.translateRu({ text: state.entry.multiline || state.entry.prompt, idea: state.entry.idea });
    box.innerHTML = translationCard(text);
    box.hidden = false;
  } catch {
    toast('Не удалось перевести промпт', 'error');
  } finally {
    setLoading(button, false);
  }
}

async function copyPrompt(text) {
  const ok = await copyText(text);
  toast(ok ? 'Промпт скопирован' : 'Браузер не дал доступ к буферу обмена', ok ? 'success' : 'error');
  if (ok) haptic();
}

/* ──────────────────────────  библиотека  ────────────────────────── */

function renderLibrary() {
  const list = $('#libraryList');
  const history = store.getHistory();
  const favorites = store.getFavorites();

  $('#historyCount').textContent = history.length;
  $('#favoritesCount').textContent = favorites.length;

  const items = state.tab === 'history' ? history : favorites;

  if (!items.length) {
    list.innerHTML = state.tab === 'history'
      ? emptyState('История пуста', 'Создайте первый промпт — он появится здесь автоматически.', '🕘')
      : emptyState('Избранного пока нет', 'Отмечайте звездой удачные промпты, чтобы быстро к ним возвращаться.', '⭐');
    return;
  }

  list.innerHTML = items.map((entry) => libraryEntry(entry, { isFavorite: store.isFavorite(entry.id) })).join('');
}

function findEntry(id) {
  return store.getHistory().find((item) => item.id === id)
    || store.getFavorites().find((item) => item.id === id);
}

/* ──────────────────────────  лимиты и аккаунт  ────────────────────────── */

function renderQuota() {
  const badge = $('#quotaBadge');
  const value = $('#quotaValue');
  const limits = quota();

  badge.classList.toggle('is-pro', limits.unlimited);
  badge.classList.toggle('is-low', !limits.unlimited && limits.left <= 2 && limits.left > 0);
  badge.classList.toggle('is-empty', limits.exhausted);

  value.textContent = limits.unlimited ? 'Premium' : `${limits.left} / ${limits.limit}`;
  badge.title = limits.unlimited
    ? 'Безлимитные генерации'
    : `Осталось ${limits.left} ${plural(limits.left, ['генерация', 'генерации', 'генераций'])} сегодня (тариф ${limits.plan.label})`;

  const user = auth.current();
  $('#accountBtn').textContent = user ? user.email.split('@')[0] : 'Войти';
}

function showPaywall(limits) {
  const user = auth.current();

  openModal({
    title: 'Лимит на сегодня исчерпан',
    subtitle: `Тариф ${limits.plan.label}: ${limits.limit} генераций в сутки.`,
    body: `
      <div class="usage">
        <div class="usage__row"><span>Использовано сегодня</span><b>${limits.used} / ${limits.limit}</b></div>
        <div class="usage__bar"><div class="usage__fill" style="width: 100%"></div></div>
      </div>
      <p class="modal__sub">
        ${user
          ? 'Premium снимает ограничение полностью и открывает до 5 вариантов промпта за раз.'
          : 'Создайте бесплатный аккаунт — лимит вырастет с 5 до 20 генераций в день.'}
      </p>
      <button class="btn btn--primary btn--block" type="button" data-action="${user ? 'upgrade' : 'auth'}">
        ${user ? 'Перейти на Premium' : 'Создать бесплатный аккаунт'}
      </button>
      <button class="btn btn--block" type="button" data-modal-dismiss>Понятно</button>`,
    onMount(modal) {
      modal.querySelector('[data-action="auth"]')?.addEventListener('click', () => openAuthModal('register'));
      modal.querySelector('[data-action="upgrade"]')?.addEventListener('click', upgradeToPremium);
      modal.querySelector('[data-modal-dismiss]')?.addEventListener('click', closeModal);
    },
  });
}

/**
 * Окно подключения собственной модели.
 *
 * Ключ сохраняется только в этом браузере и уходит только в Ollama.
 * Перед сохранением делается пробный запрос: если он не проходит,
 * пользователь сразу видит причину, а не молчаливый отказ.
 */
function openConnectModal() {
  const current = readConnection();

  openModal({
    title: 'Подключить свой AI',
    subtitle: 'Промпты будет писать модель Ollama вместо встроенного движка.',
    body: `
      <div class="field">
        <label class="field__label" for="connMode">Откуда брать модель</label>
        <select class="field__input" id="connMode">
          <option value="cloud">Облако Ollama — по ключу</option>
          <option value="local">Ollama на этом компьютере</option>
        </select>
      </div>

      <div class="field" id="keyField">
        <label class="field__label" for="connKey">Ключ с ollama.com/settings/keys</label>
        <input class="field__input" id="connKey" type="password" autocomplete="off" spellcheck="false" placeholder="вставьте ключ сюда">
      </div>

      <div class="field">
        <label class="field__label" for="connModel">Модель</label>
        <input class="field__input" id="connModel" type="text" autocomplete="off" spellcheck="false" placeholder="gpt-oss:120b-cloud">
      </div>

      <p class="field__error" id="connError"></p>
      <div id="connResult"></div>

      <button class="btn btn--primary btn--block" type="button" id="connTest">
        <span class="btn__label">Проверить и подключить</span><span class="btn__spinner"></span>
      </button>
      ${current.mode !== 'off' ? '<button class="btn btn--block btn--danger" type="button" id="connOff">Отключить</button>' : ''}

      <p class="modal__sub">
        Ключ сохраняется только в этом браузере и отправляется только в Ollama.
        Мы его не видим, в код сайта он не попадает.
      </p>`,
    onMount(modal) {
      const mode = modal.querySelector('#connMode');
      const key = modal.querySelector('#connKey');
      const model = modal.querySelector('#connModel');
      const keyField = modal.querySelector('#keyField');
      const error = modal.querySelector('#connError');
      const result = modal.querySelector('#connResult');
      const test = modal.querySelector('#connTest');

      mode.value = current.mode === 'local' ? 'local' : 'cloud';
      key.value = current.apiKey;
      model.value = current.model;

      const sync = () => {
        keyField.hidden = mode.value === 'local';
        model.placeholder = mode.value === 'local' ? 'llama3.2' : 'gpt-oss:120b-cloud';
      };
      sync();
      mode.addEventListener('change', sync);

      test.addEventListener('click', async () => {
        error.textContent = '';
        result.innerHTML = '';
        setLoading(test, true);

        // Настройки нужно сохранить до проверки: клиент читает их сам.
        const connection = saveConnection({
          mode: mode.value,
          apiKey: mode.value === 'local' ? '' : key.value.trim(),
          model: model.value.trim(),
        });
        resetProvider();

        try {
          await directProvider.ping();
          renderQuota();
          showEngine();
          result.innerHTML = '<p class="conn-ok">Модель отвечает. AI подключён.</p>';
          toast('AI подключён — промпты пишет модель', 'success', 3200);
          setTimeout(closeModal, 1200);
        } catch (failure) {
          clearConnection();
          resetProvider();
          showEngine();
          error.textContent = explain(failure, connection);
        } finally {
          setLoading(test, false);
        }
      });

      modal.querySelector('#connOff')?.addEventListener('click', () => {
        clearConnection();
        resetProvider();
        closeModal();
        showEngine();
        toast('Вернулись на встроенный движок', 'info');
      });
    },
  });
}

function openAccountModal() {
  const user = auth.current();
  if (!user) {
    openAuthModal('login');
    return;
  }

  const limits = quota();
  const plan = currentPlan();

  openModal({
    title: 'Ваш аккаунт',
    body: `
      <div class="account-card">
        <span class="avatar">${escapeHtml(user.email[0].toUpperCase())}</span>
        <div>
          <p class="account-card__email">${escapeHtml(user.email)}</p>
          <p class="account-card__plan">Тариф ${plan.label}</p>
        </div>
      </div>

      <div class="usage">
        <div class="usage__row">
          <span>Генераций сегодня</span>
          <b>${limits.unlimited ? 'Безлимит' : `${limits.used} / ${limits.limit}`}</b>
        </div>
        <div class="usage__bar"><div class="usage__fill" style="width: ${Math.round(limits.ratio * 100)}%"></div></div>
      </div>

      ${plan.id === PLANS.premium.id
        ? ''
        : `<button class="btn btn--primary btn--block" type="button" data-action="upgrade">${icon('crown')} Перейти на Premium</button>`}
      <button class="btn btn--block btn--danger" type="button" data-action="logout">${icon('logout')} Выйти</button>`,
    onMount(modal) {
      modal.querySelector('[data-action="upgrade"]')?.addEventListener('click', upgradeToPremium);
      modal.querySelector('[data-action="logout"]')?.addEventListener('click', () => {
        auth.logout();
        closeModal();
        renderQuota();
        renderPricing();
        toast('Вы вышли из аккаунта', 'info');
      });
    },
  });
}

function openAuthModal(mode = 'login') {
  const isLogin = mode === 'login';

  openModal({
    title: isLogin ? 'Вход' : 'Создать аккаунт',
    subtitle: isLogin
      ? 'Войдите, чтобы синхронизировать лимит и тариф.'
      : `Бесплатно: ${PLANS.free.dailyLimit} генераций в день.`,
    body: `
      <form class="modal__body" id="authForm" novalidate>
        <div class="field">
          <label class="field__label" for="authEmail">E-mail</label>
          <input class="field__input" id="authEmail" type="email" inputmode="email" autocomplete="email" placeholder="you@example.com" required>
        </div>
        <div class="field">
          <label class="field__label" for="authPassword">Пароль</label>
          <input class="field__input" id="authPassword" type="password" autocomplete="${isLogin ? 'current-password' : 'new-password'}" placeholder="Минимум 6 символов" required>
        </div>
        <p class="field__error" id="authError"></p>
        <button class="btn btn--primary btn--block" type="submit" id="authSubmit">
          <span class="btn__label">${isLogin ? 'Войти' : 'Создать аккаунт'}</span><span class="btn__spinner"></span>
        </button>
        <p class="modal__switch">
          ${isLogin ? 'Ещё нет аккаунта?' : 'Уже зарегистрированы?'}
          <button type="button" data-action="switch">${isLogin ? 'Создать' : 'Войти'}</button>
        </p>
      </form>`,
    onMount(modal) {
      modal.querySelector('[data-action="switch"]').addEventListener('click', () => {
        openAuthModal(isLogin ? 'register' : 'login');
      });

      let pending = false;

      const submitAuth = async (event) => {
        event.preventDefault();
        if (pending) return;
        pending = true;

        const email = modal.querySelector('#authEmail').value;
        const password = modal.querySelector('#authPassword').value;
        const errorNode = modal.querySelector('#authError');
        const submit = modal.querySelector('#authSubmit');

        errorNode.textContent = '';
        setLoading(submit, true);

        try {
          await (isLogin ? auth.login(email, password) : auth.register(email, password));
          closeModal();
          renderQuota();
          renderPricing();
          toast(isLogin ? 'С возвращением!' : 'Аккаунт создан. Лимит увеличен', 'success');
        } catch (error) {
          errorNode.textContent = error.message;
          setLoading(submit, false);
          pending = false;
        }
      };

      // Как и в генераторе: клик по кнопке — основной путь, submit — запасной.
      modal.querySelector('#authSubmit').addEventListener('click', submitAuth);
      modal.querySelector('#authForm').addEventListener('submit', submitAuth);
    },
  });
}

function upgradeToPremium() {
  if (!auth.current()) {
    openAuthModal('register');
    return;
  }

  auth.setPlan(PLANS.premium.id);
  closeModal();
  renderQuota();
  renderPricing();
  toast('Premium активирован: генерации без ограничений', 'success', 3200);
}

function renderPricing() {
  $('#pricingGrid').innerHTML = pricingCards(currentPlan().id);
  observeReveals();
}

/* ──────────────────────────  рендер настроек  ────────────────────────── */

function renderStyles() {
  $('#styleGrid').innerHTML = styleCards(state.settings.style);
}

function renderSettings() {
  $('#settingsGrid').innerHTML = settingGroups(state.settings);
}

/* ──────────────────────────  эффекты интерфейса  ────────────────────────── */

let revealObserver = null;

function observeReveals() {
  if (!('IntersectionObserver' in window)) {
    $$('[data-reveal]').forEach((node) => node.classList.add('is-in'));
    return;
  }

  revealObserver ||= new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (!entry.isIntersecting) continue;
      entry.target.classList.add('is-in');
      revealObserver.unobserve(entry.target);
    }
  }, { rootMargin: '0px 0px -40px 0px', threshold: 0.08 });

  $$('[data-reveal]:not(.is-in)').forEach((node) => revealObserver.observe(node));
}

function autoGrow(textarea) {
  textarea.style.height = 'auto';
  textarea.style.height = `${Math.min(textarea.scrollHeight, 320)}px`;
}

/* ──────────────────────────  обработчики  ────────────────────────── */

function bindComposer() {
  const input = $('#ideaInput');
  const counter = $('#charCount');

  // Отправка формы блокируется, если страница открыта внутри песочницы
  // (iframe c sandbox без allow-forms) — событие submit туда просто не доходит.
  // Поэтому основной путь запуска — клик по кнопке, а submit оставлен
  // запасным для клавиатуры.
  const start = (event) => {
    event.preventDefault();
    generate();
  };

  $('#generateBtn').addEventListener('click', start);
  $('#promptForm').addEventListener('submit', start);

  input.addEventListener('input', () => {
    counter.textContent = input.value.length;
    autoGrow(input);
  });

  // Ctrl/Cmd + Enter — быстрая отправка с клавиатуры.
  input.addEventListener('keydown', (event) => {
    if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
      event.preventDefault();
      generate();
    }
  });

  input.setAttribute('maxlength', String(LIMITS.ideaMaxLength));

  $('#engineHint').addEventListener('click', (event) => {
    if (event.target.closest('[data-action="connect"]')) openConnectModal();
  });

  $('#diceBtn').addEventListener('click', () => {
    const idea = pick(IDEA_SEEDS.filter((seed) => seed !== input.value));
    input.value = idea;
    counter.textContent = idea.length;
    autoGrow(input);
    input.focus();
    haptic();
  });
}

function bindSelectors() {
  $('#styleGrid').addEventListener('click', (event) => {
    const card = event.target.closest('[data-style]');
    if (!card) return;

    state.settings = store.patchSettings({ style: card.dataset.style });
    renderStyles();
    haptic();
  });

  $('#settingsGrid').addEventListener('click', (event) => {
    const chip = event.target.closest('[data-option]');
    if (!chip) return;

    state.settings = store.patchSettings({ [chip.dataset.group]: chip.dataset.option });
    renderSettings();
    haptic();
  });

  $('#resetStyleBtn').addEventListener('click', () => {
    state.settings = store.patchSettings({ style: 'realism' });
    renderStyles();
  });

  $('#resetSettingsBtn').addEventListener('click', () => {
    state.settings = store.resetSettings();
    renderStyles();
    renderSettings();
    toast('Настройки сброшены', 'info');
  });
}

function bindResultActions() {
  $('#resultZone').addEventListener('click', async (event) => {
    const trigger = event.target.closest('[data-action]');
    if (!trigger) return;

    const { action } = trigger.dataset;

    if (action === 'copy-variant') {
      const text = trigger.closest('.variant').querySelector('.variant__text').textContent;
      copyPrompt(text);
      return;
    }
    if (!state.entry && action !== 'open-account') return;

    switch (action) {
      case 'copy':      copyPrompt(state.entry.prompt); break;
      case 'improve':   improvePrompt(trigger); break;
      case 'variants':  makeVariants(trigger); break;
      case 'translate': translateCurrent(trigger); break;
      case 'translate-ru': showRussian(trigger); break;
      case 'regenerate': generate(state.entry.idea); break;
      case 'open-account': openAccountModal(); break;
      case 'favorite': {
        const added = store.toggleFavorite(state.entry);
        renderResult();
        toast(added ? 'Добавлено в избранное' : 'Убрано из избранного', 'success');
        break;
      }
      default: break;
    }
  });
}

function bindLibrary() {
  $$('.tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      state.tab = tab.dataset.tab;
      $$('.tab').forEach((item) => {
        const active = item === tab;
        item.classList.toggle('is-active', active);
        item.setAttribute('aria-selected', String(active));
      });
      renderLibrary();
    });
  });

  $('#libraryList').addEventListener('click', (event) => {
    const trigger = event.target.closest('[data-action]');
    if (!trigger) return;

    const id = trigger.closest('[data-entry]')?.dataset.entry;
    const entry = findEntry(id);
    if (!entry) return;

    switch (trigger.dataset.action) {
      case 'copy-entry': copyPrompt(entry.prompt); break;
      case 'fav-entry': {
        const added = store.toggleFavorite(entry);
        renderLibrary();
        toast(added ? 'Добавлено в избранное' : 'Убрано из избранного', 'success');
        break;
      }
      case 'reuse-entry': {
        state.entry = entry;
        state.settings = store.patchSettings(entry.settings || {});
        $('#ideaInput').value = entry.idea;
        $('#charCount').textContent = entry.idea.length;
        renderStyles();
        renderSettings();
        renderResult();
        break;
      }
      case 'delete-entry': {
        store.removeHistory(entry.id);
        store.removeFavorite(entry.id);
        if (state.entry?.id === entry.id) {
          state.entry = null;
          renderResult();
        }
        renderLibrary();
        toast('Промпт удалён', 'info');
        break;
      }
      default: break;
    }
  });

  $('#clearHistoryBtn').addEventListener('click', () => {
    if (!store.getHistory().length) {
      toast('История уже пуста', 'info');
      return;
    }
    store.clearHistory();
    renderLibrary();
    toast('История очищена', 'info');
  });
}

function bindAccount() {
  $('#accountBtn').addEventListener('click', openAccountModal);
  $('#quotaBadge').addEventListener('click', openAccountModal);

  $('#pricingGrid').addEventListener('click', (event) => {
    const button = event.target.closest('[data-plan]');
    if (!button) return;

    if (button.dataset.plan === PLANS.premium.id) upgradeToPremium();
    else if (!auth.current()) openAuthModal('register');
  });
}

function bindChrome() {
  const header = $('#header');
  const nav = $('#nav');
  const burger = $('#burger');
  const fab = $('#fabTop');

  const onScroll = () => {
    header.classList.toggle('is-stuck', window.scrollY > 8);
    fab.classList.toggle('is-visible', window.scrollY > 600);
  };
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  burger.addEventListener('click', () => {
    const open = nav.classList.toggle('is-open');
    burger.setAttribute('aria-expanded', String(open));
  });

  $$('[data-close-menu]').forEach((link) => {
    link.addEventListener('click', () => {
      nav.classList.remove('is-open');
      burger.setAttribute('aria-expanded', 'false');
    });
  });

  fab.addEventListener('click', () => {
    $('#ideaInput').scrollIntoView({ behavior: 'smooth', block: 'center' });
    setTimeout(() => $('#ideaInput').focus({ preventScroll: true }), 400);
  });

  window.addEventListener('resize', debounce(() => autoGrow($('#ideaInput')), 150));
}

/* ──────────────────────────  инициализация  ────────────────────────── */

/**
 * Сообщает, что подключён настоящий AI.
 *
 * Если бэкенда нет, подсказка остаётся прежней: встроенный движок — это
 * штатный режим, а не поломка, пугать пользователя нечем.
 */
async function showEngine() {
  const hint = $('#engineHint');
  if (!hint) return;

  try {
    const { info } = await ai.status();

    if (!info) {
      hint.innerHTML = `Промпты собирает встроенный движок.
        <button class="link-btn" type="button" data-action="connect">Подключить свой AI</button>`;
      return;
    }

    const name = info.model || (info.mode === 'local' ? 'Ollama на этом компьютере' : 'Ollama');
    hint.innerHTML = `<span class="engine"><span class="engine__dot"></span>AI подключён: ${escapeHtml(name)}</span>
      <button class="link-btn" type="button" data-action="connect">изменить</button>`;
  } catch {
    /* молча остаёмся на встроенном движке */
  }
}

function init() {
  $('#year').textContent = new Date().getFullYear();

  renderStyles();
  renderSettings();
  renderLibrary();
  renderQuota();
  renderPricing();

  $('#featureGrid').innerHTML = featureCards();
  $('#faqList').innerHTML = faqItems();

  bindComposer();
  bindSelectors();
  bindResultActions();
  bindLibrary();
  bindAccount();
  bindChrome();

  observeReveals();
  showEngine();

  // Состояние может измениться из другой вкладки — держим счётчики в актуальном виде.
  store.subscribe((event) => {
    if (event.type === 'usage' || event.type === 'session') renderQuota();
    if (event.type === 'history' || event.type === 'favorites') renderLibrary();
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init, { once: true });
} else {
  init();
}
