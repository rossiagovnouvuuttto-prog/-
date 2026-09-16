/* ==============================================================
   Multi AI Chat - frontend
   The Hugging Face token lives on the server only. This file never
   sees it: every completion goes through POST /api/chat.
   ============================================================== */
'use strict';

const $  = (id) => document.getElementById(id);
const el = (tag, cls, text) => {
  const n = document.createElement(tag);
  if (cls) n.className = cls;
  if (text != null) n.textContent = text;
  return n;
};
const esc = (s) => String(s)
  .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 8);

/* ============== Storage ============== */
const LS = {
  chats:    'mac.chats',
  current:  'mac.current',
  settings: 'mac.settings',
  model:    'mac.model',
  custom:   'mac.customModels',
};

const THEMES = [
  { id: 'pokemon',   name: 'Pokémon' },
  { id: 'anime',     name: 'Аниме' },
  { id: 'minecraft', name: 'Minecraft' },
  { id: 'roblox',    name: 'Roblox' },
  { id: 'gta',       name: 'GTA SA' },
];

const DEFAULT_SETTINGS = {
  theme: 'pokemon',
  systemPrompt: 'Ты — полезный ИИ-ассистент. Отвечай понятно, по существу и на языке пользователя. Оформляй код в Markdown-блоках.',
  temperature: 0.7,
  maxTokens: 2048,
  topP: 0.95,
};

function load(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw == null ? fallback : JSON.parse(raw);
  } catch { return fallback; }
}
function save(key, value) {
  try { localStorage.setItem(key, JSON.stringify(value)); }
  catch { toast('err', 'Хранилище переполнено', 'Не удалось сохранить историю. Удалите старые чаты.'); }
}

/* ============== State ============== */
const state = {
  chats: load(LS.chats, []),
  currentId: load(LS.current, null),
  settings: Object.assign({}, DEFAULT_SETTINGS, load(LS.settings, {})),
  catalog: { default: 'deepseek-ai/DeepSeek-V3-0324', categories: [] },
  featured: [],
  customModels: load(LS.custom, []),
  model: load(LS.model, null),
  activeCat: 'all',
  filter: '',
  modelFilter: '',
  abort: null,
  renameTarget: null,
};

function applyTheme(id) {
  const known = THEMES.some((t) => t.id === id) ? id : 'pokemon';
  document.documentElement.dataset.theme = known;
}
applyTheme(state.settings.theme);

const currentChat = () => state.chats.find((c) => c.id === state.currentId) || null;
const persistChats = () => { save(LS.chats, state.chats); save(LS.current, state.currentId); };

/* ============== Toasts ============== */
const TOAST_ICONS = {
  err:  '<path d="M12 8v5M12 16.5v.5"/><circle cx="12" cy="12" r="9"/>',
  ok:   '<path d="M20 6L9 17l-5-5"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 7.5v.5"/>',
};
function toast(kind, title, text, ms = 6000) {
  const box = $('toasts');
  if (!box) return;
  const t = el('div', `toast ${kind}`);
  t.innerHTML =
    `<svg class="ico" viewBox="0 0 24 24">${TOAST_ICONS[kind] || TOAST_ICONS.info}</svg>` +
    '<div class="t-body"><div class="t-title"></div><div class="t-text"></div></div>' +
    '<button class="t-close" aria-label="Закрыть">&#10005;</button>';
  t.querySelector('.t-title').textContent = title;
  t.querySelector('.t-text').textContent = text || '';
  const close = () => { t.classList.add('out'); setTimeout(() => t.remove(), 220); };
  t.querySelector('.t-close').onclick = close;
  box.appendChild(t);
  if (ms) setTimeout(close, ms);
}

async function copyText(text, okMsg = 'Скопировано') {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
    } else {
      const ta = el('textarea');
      ta.value = text;
      ta.style.cssText = 'position:fixed;opacity:0;pointer-events:none';
      document.body.appendChild(ta); ta.select();
      document.execCommand('copy'); ta.remove();
    }
    toast('ok', okMsg, '', 1800);
    return true;
  } catch {
    toast('err', 'Не удалось скопировать', 'Браузер заблокировал доступ к буферу обмена.');
    return false;
  }
}

/* ==============================================================
   Markdown renderer (self-contained - no external libraries)
   ============================================================== */
const KEYWORDS = {
  common: 'if else for while return function class const let var new this import from export default async await try catch finally throw switch case break continue typeof instanceof in of do yield static extends super null true false undefined',
  python: 'def class return if elif else for while import from as try except finally raise with lambda yield pass break continue global nonlocal assert del is not and or in None True False self async await match case',
  go: 'func package import return if else for range var const type struct interface map chan go defer select switch case break continue nil true false',
  rust: 'fn let mut const struct enum impl trait pub use mod match if else for while loop return break continue self Self where as dyn ref move async await true false Some None Ok Err',
  java: 'public private protected class interface extends implements static final void return if else for while new this super try catch finally throw throws import package int long double float boolean char String true false null',
  sql: 'select from where join inner left right outer on group by order having limit offset insert into values update set delete create table alter drop index as and or not null distinct count sum avg min max case when then end',
  css: 'important media supports keyframes import charset font-face root',
  bash: 'if then else elif fi for while do done case esac function return echo export local read source cd exit',
};
const LANG_ALIAS = {
  js: 'common', javascript: 'common', ts: 'common', typescript: 'common',
  jsx: 'common', tsx: 'common', json: 'common', c: 'java', cpp: 'java',
  'c++': 'java', cs: 'java', csharp: 'java', kotlin: 'java', swift: 'java',
  php: 'common', ruby: 'python', rb: 'python', py: 'python', sh: 'bash',
  shell: 'bash', zsh: 'bash', yaml: 'python', yml: 'python', html: 'css', xml: 'css',
};
const HASH_COMMENT = new Set(['python', 'py', 'bash', 'sh', 'shell', 'zsh', 'yaml', 'yml', 'ruby', 'rb', 'toml', 'ini', 'r', 'perl']);
const DASH_COMMENT = new Set(['sql', 'lua', 'haskell', 'hs', 'ada']);

function highlight(code, lang) {
  const key = (lang || '').toLowerCase();
  const words = KEYWORDS[key] || KEYWORDS[LANG_ALIAS[key]] || KEYWORDS.common;
  const kw = new Set(words.split(' '));

  let comment = '\\/\\*[\\s\\S]*?\\*\\/|\\/\\/[^\\n]*';
  if (HASH_COMMENT.has(key)) comment = '#[^\\n]*';
  else if (DASH_COMMENT.has(key)) comment = '--[^\\n]*|\\/\\*[\\s\\S]*?\\*\\/';

  const re = new RegExp(
    '(' + comment + ')' +
    '|("(?:\\\\.|[^"\\\\])*"|\'(?:\\\\.|[^\'\\\\])*\')' +
    '|(\\b\\d+(?:\\.\\d+)?\\b)' +
    '|([A-Za-z_$][\\w$]*)(?=\\s*\\()' +
    '|([A-Za-z_$][\\w$]*)',
    'g',
  );

  let out = '', last = 0, m;
  while ((m = re.exec(code)) !== null) {
    out += esc(code.slice(last, m.index));
    const body = esc(m[0]);
    if (m[1])      out += `<span class="tok-com">${body}</span>`;
    else if (m[2]) out += `<span class="tok-str">${body}</span>`;
    else if (m[3]) out += `<span class="tok-num">${body}</span>`;
    else if (m[4]) out += kw.has(m[4]) ? `<span class="tok-kw">${body}</span>` : `<span class="tok-fn">${body}</span>`;
    else if (m[5]) out += kw.has(m[5]) ? `<span class="tok-kw">${body}</span>` : body;
    else           out += body;
    last = m.index + m[0].length;
  }
  return out + esc(code.slice(last));
}

const safeUrl = (u) => (/^(https?:\/\/|mailto:|\/|#)/i.test(u.trim()) ? u.trim() : '#');

const CODE_MARK = '';   // private-use sentinels, never present in model output
const FENCE_MARK = '';

function inline(src) {
  const code = [];
  let s = String(src).replace(/`([^`\n]+)`/g, (_, c) => CODE_MARK + (code.push(c) - 1) + CODE_MARK);
  s = esc(s);
  s = s.replace(/!\[([^\]]*)\]\(([^)\s]+)[^)]*\)/g,
    (_, alt, u) => `<a href="${esc(safeUrl(u))}" target="_blank" rel="noopener noreferrer">[изображение] ${alt || ''}</a>`);
  s = s.replace(/\[([^\]]+)\]\(([^)\s]+)[^)]*\)/g,
    (_, txt, u) => `<a href="${esc(safeUrl(u))}" target="_blank" rel="noopener noreferrer">${txt}</a>`);
  s = s.replace(/(^|[\s(])(https?:\/\/[^\s<)]+)/g,
    (_, pre, u) => `${pre}<a href="${esc(u)}" target="_blank" rel="noopener noreferrer">${u}</a>`);
  s = s.replace(/\*\*\*([^*]+)\*\*\*/g, '<strong><em>$1</em></strong>');
  s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  s = s.replace(/(^|\W)__([^_]+)__(?=\W|$)/g, '$1<strong>$2</strong>');
  s = s.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>');
  s = s.replace(/(^|\W)_([^_\n]+)_(?=\W|$)/g, '$1<em>$2</em>');
  s = s.replace(/~~([^~]+)~~/g, '<del>$1</del>');
  return s.replace(new RegExp(CODE_MARK + '(\\d+)' + CODE_MARK, 'g'),
    (_, i) => `<code>${esc(code[+i])}</code>`);
}

function renderMarkdown(src) {
  if (!src) return '';
  // An unterminated fence mid-stream still renders as a code block.
  if (((src.match(/^```/gm) || []).length) % 2 === 1) src += '\n```';

  const fences = [];
  src = src.replace(/^```([^\n`]*)\n?([\s\S]*?)^```[ \t]*$/gm, (_, lang, code) => {
    fences.push({ lang: (lang || '').trim(), code: code.replace(/\n$/, '') });
    return FENCE_MARK + (fences.length - 1) + FENCE_MARK;
  });

  const fenceLine = new RegExp('^' + FENCE_MARK + '(\\d+)' + FENCE_MARK + '$');
  const lines = src.split('\n');
  const out = [];
  let i = 0;

  const isList  = (l) => /^\s*(?:[-*+]|\d+[.)])\s+/.test(l);
  const isTable = (l, n) => !!l && l.includes('|') && !!n &&
                            /^\s*\|?[\s:.|-]*-[\s:|.-]*\|?\s*$/.test(n) && n.includes('-');

  while (i < lines.length) {
    const line = lines[i];
    if (!line.trim()) { i++; continue; }

    const fence = line.match(fenceLine);
    if (fence) {
      const { lang, code } = fences[+fence[1]];
      out.push(
        '<div class="code-block"><div class="code-head">' +
        `<span class="code-lang">${esc(lang || 'code')}</span>` +
        `<button class="code-copy" type="button" data-code="${encodeURIComponent(code)}">` +
        '<svg viewBox="0 0 24 24"><rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15V5a2 2 0 012-2h10"/></svg>' +
        'Копировать код</button></div>' +
        `<pre><code>${highlight(code, lang)}</code></pre></div>`,
      );
      i++; continue;
    }

    const h = line.match(/^(#{1,6})\s+(.*)$/);
    if (h) {
      const lvl = Math.min(h[1].length, 4);
      out.push(`<h${lvl}>${inline(h[2].trim())}</h${lvl}>`);
      i++; continue;
    }

    if (/^\s*(?:---+|\*\*\*+|___+)\s*$/.test(line)) { out.push('<hr>'); i++; continue; }

    if (/^\s*>\s?/.test(line)) {
      const buf = [];
      while (i < lines.length && /^\s*>\s?/.test(lines[i])) buf.push(lines[i++].replace(/^\s*>\s?/, ''));
      out.push(`<blockquote>${renderMarkdown(buf.join('\n'))}</blockquote>`);
      continue;
    }

    if (isTable(line, lines[i + 1])) {
      const cells = (row) => row.replace(/^\s*\|/, '').replace(/\|\s*$/, '').split('|').map((c) => c.trim());
      const head = cells(line);
      i += 2;
      const body = [];
      while (i < lines.length && lines[i].includes('|') && lines[i].trim()) body.push(cells(lines[i++]));
      out.push(
        `<div class="table-wrap"><table><thead><tr>${head.map((c) => `<th>${inline(c)}</th>`).join('')}</tr></thead>` +
        `<tbody>${body.map((r) => `<tr>${head.map((_, k) => `<td>${inline(r[k] || '')}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`,
      );
      continue;
    }

    if (isList(line)) {
      const buf = [];
      while (i < lines.length && (isList(lines[i]) || (/^\s{2,}\S/.test(lines[i]) && buf.length))) buf.push(lines[i++]);
      out.push(buildList(buf));
      continue;
    }

    const para = [];
    while (i < lines.length && lines[i].trim() && !isList(lines[i]) &&
           !/^(#{1,6})\s/.test(lines[i]) && !/^\s*>/.test(lines[i]) &&
           !fenceLine.test(lines[i]) && !isTable(lines[i], lines[i + 1])) {
      para.push(lines[i++]);
    }
    if (para.length) out.push(`<p>${inline(para.join('\n')).replace(/\n/g, '<br>')}</p>`);
  }
  return out.join('');
}

/** Builds a (possibly nested) list from raw markdown lines. */
function buildList(lines) {
  const indentOf = (l) => l.match(/^\s*/)[0].replace(/\t/g, '  ').length;
  const bullets = lines.filter((l) => /^\s*(?:[-*+]|\d+[.)])\s+/.test(l));
  const base = bullets.length ? Math.min(...bullets.map(indentOf)) : 0;
  const ordered = /^\s*\d+[.)]\s+/.test(bullets[0] || '');
  const items = [];

  for (const line of lines) {
    const m = line.match(/^(\s*)(?:[-*+]|\d+[.)])\s+(.*)$/);
    if (m && indentOf(line) <= base) items.push({ text: m[2], children: [] });
    else if (items.length) items[items.length - 1].children.push(line.slice(base + 2));
    else items.push({ text: line.trim(), children: [] });
  }

  const html = items.map((it) => {
    let inner = inline(it.text);
    const kids = it.children.filter((l) => l.trim());
    if (kids.length) {
      inner += /^\s*(?:[-*+]|\d+[.)])\s+/.test(kids[0]) ? buildList(kids) : `<br>${inline(kids.join(' '))}`;
    }
    return `<li>${inner}</li>`;
  }).join('');

  return ordered ? `<ol>${html}</ol>` : `<ul>${html}</ul>`;
}

/* ==============================================================
   Model catalogue
   ============================================================== */
function allModels() {
  const list = [];
  for (const cat of state.catalog.categories || []) {
    for (const m of cat.models || []) list.push({ ...m, cat: cat.id, catName: cat.name, icon: cat.icon });
  }
  for (const m of state.customModels) {
    list.push({ ...m, cat: 'custom', catName: 'Мои модели', icon: '⭐', custom: true });
  }
  return list;
}
function modelLabel(id) {
  const star = state.featured.find((f) => f.id === id);
  if (star) return `${star.icon} ${star.name}`;
  const hit = allModels().find((m) => m.id === id);
  return hit ? hit.name : (id || '—');
}

/* ==============================================================
   Starter cards - the four featured models
   The backend resolves each family to a Model ID that Hugging Face
   actually serves, and tells us whether it is online.
   ============================================================== */
async function loadFeatured() {
  try {
    const res = await fetch('/api/featured');
    if (!res.ok) throw new Error(String(res.status));
    const data = await res.json();
    state.featured = Array.isArray(data.featured) ? data.featured : [];
  } catch {
    state.featured = [];
  }
}

function starterCard(f) {
  const card = el('button', 'starter');
  card.dataset.type = f.type || 'normal';
  card.dataset.key = f.key || '';
  if (f.id === state.model) card.classList.add('chosen');
  const online = f.status === 'online';

  card.innerHTML =
    '<span class="s-shine" aria-hidden="true"></span>' +
    '<span class="s-icon"></span>' +
    '<span class="s-body"><span class="s-name"></span><span class="s-desc"></span></span>' +
    `<span class="s-status ${online ? 'on' : 'off'}"><i></i><span></span></span>` +
    '<span class="s-id"></span>';

  card.querySelector('.s-icon').textContent = f.icon || '';
  card.querySelector('.s-name').textContent = f.name || '';
  card.querySelector('.s-desc').textContent = f.desc || '';
  card.querySelector('.s-status span').textContent = online ? 'Online' : 'Offline';
  card.querySelector('.s-id').textContent = f.id || '';
  card.title = f.id || '';

  card.onclick = () => {
    setModel(f.id, { silent: true });
    if (online) {
      toast('ok', `${f.icon} ${f.name} выбран`, f.id, 2600);
    } else {
      toast('err', `${f.name} сейчас офлайн`,
        'Модель сейчас недоступна через Hugging Face Inference. Выберите другую карточку.', 7000);
    }
    closeModal('modelModal');
    renderChat();
    renderStarters();
    $('input').focus();
  };
  return card;
}

/** Repaints every starter grid currently on the page. */
function renderStarters() {
  for (const box of document.querySelectorAll('.starters')) {
    box.innerHTML = '';
    if (!state.featured.length) {
      box.appendChild(el('div', 'starters-empty', 'Не удалось загрузить список моделей.'));
      continue;
    }
    for (const f of state.featured) box.appendChild(starterCard(f));
  }
}

/* ==============================================================
   Sidebar
   ============================================================== */
const ICON_BUBBLE = '<svg class="chat-ico" viewBox="0 0 24 24"><path d="M21 12a8 8 0 01-8 8H7l-4 3v-5.5A8 8 0 1121 12z"/></svg>';
const ICON_PEN    = '<svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 013 3L7 19l-4 1 1-4z"/></svg>';
const ICON_TRASH  = '<svg viewBox="0 0 24 24"><path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"/></svg>';

function renderSidebar() {
  const box = $('chatList');
  box.innerHTML = '';

  const q = state.filter.trim().toLowerCase();
  const chats = [...state.chats]
    .sort((a, b) => b.updatedAt - a.updatedAt)
    .filter((c) => {
      if (!q) return true;
      if (c.title.toLowerCase().includes(q)) return true;
      return c.messages.some((m) => (m.content || '').toLowerCase().includes(q));
    });

  if (!chats.length) {
    box.appendChild(el('div', 'empty-history',
      q ? 'Ничего не найдено' : 'Пока нет чатов.\nНачните первый разговор.'));
    return;
  }

  for (const chat of chats) {
    const item = el('div', 'chat-item' + (chat.id === state.currentId ? ' active' : ''));
    item.innerHTML = ICON_BUBBLE;

    const title = el('button', 'chat-title', chat.title);
    title.title = chat.title;
    title.onclick = () => { openChat(chat.id); closeSidebar(); };
    item.appendChild(title);

    const acts = el('div', 'chat-acts');
    const ren = el('button', 'ren'); ren.innerHTML = ICON_PEN; ren.title = 'Переименовать';
    ren.onclick = (e) => { e.stopPropagation(); askRename(chat.id); };
    const del = el('button', 'del'); del.innerHTML = ICON_TRASH; del.title = 'Удалить';
    del.onclick = (e) => { e.stopPropagation(); deleteChat(chat.id); };
    acts.append(ren, del);
    item.appendChild(acts);

    box.appendChild(item);
  }
}

/* ==============================================================
   Chat lifecycle
   ============================================================== */
function newChat() {
  stopStream();
  const chat = {
    id: uid(),
    title: 'Новый чат',
    model: state.model,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    messages: [],
  };
  state.chats.unshift(chat);
  state.currentId = chat.id;
  persistChats();
  renderSidebar();
  renderChat();
  $('input').focus();
  return chat;
}

function openChat(id) {
  stopStream();
  state.currentId = id;
  const chat = currentChat();
  if (chat && chat.model) setModel(chat.model, { silent: true, persistChat: false });
  persistChats();
  renderSidebar();
  renderChat();
}

function deleteChat(id) {
  const chat = state.chats.find((c) => c.id === id);
  if (!chat) return;
  if (!confirm(`Удалить чат «${chat.title}»?`)) return;
  state.chats = state.chats.filter((c) => c.id !== id);
  if (state.currentId === id) state.currentId = state.chats.length ? state.chats[0].id : null;
  persistChats();
  renderSidebar();
  renderChat();
  toast('ok', 'Чат удалён', '', 2200);
}

function clearAllChats() {
  if (!state.chats.length) { toast('info', 'История уже пуста', '', 2200); return; }
  if (!confirm('Удалить всю историю чатов? Это действие нельзя отменить.')) return;
  stopStream();
  state.chats = [];
  state.currentId = null;
  persistChats();
  renderSidebar();
  renderChat();
  toast('ok', 'История очищена', '', 2200);
}

function askRename(id) {
  const chat = state.chats.find((c) => c.id === id);
  if (!chat) return;
  state.renameTarget = id;
  $('renameInput').value = chat.title;
  openModal('renameModal');
  setTimeout(() => { $('renameInput').focus(); $('renameInput').select(); }, 60);
}

function commitRename() {
  const chat = state.chats.find((c) => c.id === state.renameTarget);
  const name = $('renameInput').value.trim();
  if (chat && name) {
    chat.title = name.slice(0, 80);
    chat.updatedAt = Date.now();
    persistChats();
    renderSidebar();
  }
  closeModal('renameModal');
}

/* ==============================================================
   Message rendering
   ============================================================== */
const ICONS = {
  copy:   '<svg viewBox="0 0 24 24"><rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15V5a2 2 0 012-2h10"/></svg>',
  redo:   '<svg viewBox="0 0 24 24"><path d="M21 12a9 9 0 11-3-6.7"/><path d="M21 4v5h-5"/></svg>',
  up:     '<svg viewBox="0 0 24 24"><path d="M7 21V10l5-8a2 2 0 013 2l-1 6h5a2 2 0 012 2.4l-1.6 7A2 2 0 0117 21z"/></svg>',
  down:   '<svg viewBox="0 0 24 24"><path d="M17 3v11l-5 8a2 2 0 01-3-2l1-6H5a2 2 0 01-2-2.4l1.6-7A2 2 0 017 3z"/></svg>',
};

const QUICK = [
  { emoji: '\u{1F4BB}', label: 'Написать код',        prompt: 'Напиши функцию на Python, которая читает CSV-файл и возвращает статистику по числовым колонкам. Добавь комментарии.' },
  { emoji: '\u{1F3AE}', label: 'Помочь с Minecraft',  prompt: 'Объясни, как сделать автоматическую ферму железа в Minecraft 1.21. Пошагово и со списком ресурсов.' },
  { emoji: '\u{1F4DA}', label: 'Объяснить тему',      prompt: 'Объясни простыми словами, как работают нейронные сети. Приведи аналогию и небольшую таблицу с терминами.' },
  { emoji: '\u{1F4A1}', label: 'Придумать идею',      prompt: 'Придумай 5 оригинальных идей для пет-проекта на выходные. Для каждой укажи стек и сложность.' },
];

function welcomeNode() {
  const w = el('div', 'welcome');
  w.innerHTML =
    '<div class="welcome-logo" aria-hidden="true"></div>' +
    '<h1>Multi AI Chat</h1>' +
    '<p>Выберите нейросеть и начните общение</p>';

  const starters = el('div', 'starters');
  w.appendChild(starters);

  const quickLabel = el('div', 'quick-label', 'Или начните с готового вопроса');
  w.appendChild(quickLabel);

  const grid = el('div', 'quick');
  for (const q of QUICK) {
    const b = el('button');
    b.innerHTML = `<span class="emoji">${q.emoji}</span><span></span>`;
    b.querySelector('span:last-child').textContent = q.label;
    b.onclick = () => {
      const ta = $('input');
      ta.value = q.prompt;
      autoGrow(ta);
      ta.focus();
      refreshSendState();
    };
    grid.appendChild(b);
  }
  w.appendChild(grid);
  return w;
}

function messageNode(msg, chat) {
  const wrap = el('div', `msg ${msg.role === 'user' ? 'user' : 'ai'}`);
  wrap.dataset.id = msg.id;

  const av = el('div', 'avatar', msg.role === 'user' ? 'Вы' : '');
  if (msg.role !== 'user') {
    av.innerHTML = '<svg viewBox="0 0 24 24" style="width:17px;height:17px"><path d="M12 3l2.2 5.8L20 11l-5.8 2.2L12 19l-2.2-5.8L4 11l5.8-2.2z"/></svg>';
  }
  wrap.appendChild(av);

  const col = el('div', 'bubble-col');
  const bubble = el('div', 'bubble' + (msg.error ? ' error' : ''));

  if (msg.role === 'user') {
    bubble.textContent = msg.content;
  } else {
    bubble.classList.add('md');
    paintAssistant(bubble, msg);
  }
  col.appendChild(bubble);

  if (msg.role === 'assistant' && !msg.streaming) {
    col.appendChild(assistantActions(msg, chat));
  }

  wrap.appendChild(col);
  return wrap;
}

function paintAssistant(bubble, msg) {
  if (msg.error) {
    bubble.innerHTML = '<span class="err-title"></span><span class="err-text"></span>';
    bubble.querySelector('.err-title').textContent = 'Ошибка';
    bubble.querySelector('.err-text').textContent = msg.content;
    return;
  }
  if (msg.streaming && !msg.content && !msg.reasoning) {
    bubble.innerHTML =
      '<span class="typing">AI печатает<span class="dots"><i></i><i></i><i></i></span></span>';
    return;
  }
  let html = '';
  if (msg.reasoning) html += `<div class="reasoning">${esc(msg.reasoning)}</div>`;
  html += renderMarkdown(msg.content || '');
  if (msg.streaming) html += '<span class="caret"></span>';
  bubble.innerHTML = html;
  wireCodeCopy(bubble);
}

function wireCodeCopy(root) {
  root.querySelectorAll('.code-copy').forEach((btn) => {
    if (btn.dataset.wired) return;
    btn.dataset.wired = '1';
    btn.onclick = () => copyText(decodeURIComponent(btn.dataset.code), 'Код скопирован');
  });
}

function assistantActions(msg, chat) {
  const bar = el('div', 'msg-acts');

  const mk = (cls, icon, label, title) => {
    const b = el('button', `act ${cls}`);
    b.innerHTML = icon + `<span>${label}</span>`;
    b.title = title || label;
    return b;
  };

  const copy = mk('', ICONS.copy, 'Копировать');
  copy.onclick = () => copyText(msg.content);
  bar.appendChild(copy);

  const redo = mk('', ICONS.redo, 'Повторить ответ');
  redo.onclick = () => regenerate(msg.id);
  bar.appendChild(redo);

  if (!msg.error) {
    const up = mk(msg.feedback === 'up' ? 'on' : '', ICONS.up, '', 'Хороший ответ');
    up.innerHTML = ICONS.up;
    up.onclick = () => setFeedback(msg, chat, 'up');
    const down = mk(msg.feedback === 'down' ? 'on down' : '', ICONS.down, '', 'Плохой ответ');
    down.innerHTML = ICONS.down;
    down.onclick = () => setFeedback(msg, chat, 'down');
    bar.append(up, down);
  }
  return bar;
}

function setFeedback(msg, chat, value) {
  msg.feedback = msg.feedback === value ? null : value;
  chat.updatedAt = Date.now();
  persistChats();
  renderChat();
  if (msg.feedback) toast('ok', msg.feedback === 'up' ? 'Спасибо за оценку' : 'Оценка учтена', '', 1600);
}

function renderChat() {
  const box = $('messages');
  const chat = currentChat();
  box.innerHTML = '';

  if (!chat || !chat.messages.length) {
    box.appendChild(welcomeNode());
    renderStarters();
    updateTopbar();
    return;
  }

  const thread = el('div', 'thread');
  for (const msg of chat.messages) thread.appendChild(messageNode(msg, chat));
  box.appendChild(thread);
  scrollToBottom(true);
  updateTopbar();
}

function nearBottom() {
  const b = $('messages');
  return b.scrollHeight - b.scrollTop - b.clientHeight < 140;
}
function scrollToBottom(force) {
  const b = $('messages');
  if (force || nearBottom()) b.scrollTop = b.scrollHeight;
}

function updateTopbar() {
  $('modelName').textContent = modelLabel(state.model);
  $('modelName').title = state.model || '';
}
function setStatus(kind, text) {
  const s = $('modelStatus');
  s.className = 'model-status' + (kind ? ` ${kind}` : '');
  $('statusText').textContent = text;
}

/* ==============================================================
   Sending / streaming
   ============================================================== */
function buildHistory(chat, upto) {
  const msgs = [];
  const sys = state.settings.systemPrompt.trim();
  if (sys) msgs.push({ role: 'system', content: sys });
  for (const m of chat.messages.slice(0, upto)) {
    if (m.error || m.streaming) continue;
    if (!m.content) continue;
    msgs.push({ role: m.role, content: m.content });
  }
  return msgs;
}

async function send(text) {
  const body = (text ?? $('input').value).trim();
  if (!body || state.abort) return;
  if (!state.model) { toast('err', 'Модель не выбрана', 'Нажмите «Выбрать модель».'); return; }

  let chat = currentChat();
  if (!chat) chat = newChat();

  chat.messages.push({ id: uid(), role: 'user', content: body });
  if (chat.title === 'Новый чат') {
    chat.title = body.replace(/\s+/g, ' ').slice(0, 48) + (body.length > 48 ? '…' : '');
  }
  chat.model = state.model;
  chat.updatedAt = Date.now();

  const ta = $('input');
  ta.value = '';
  autoGrow(ta);
  refreshSendState();

  const reply = { id: uid(), role: 'assistant', content: '', streaming: true };
  chat.messages.push(reply);

  persistChats();
  renderSidebar();
  renderChat();

  await runCompletion(chat, reply);
}

async function regenerate(msgId) {
  if (state.abort) return;
  const chat = currentChat();
  if (!chat) return;
  const idx = chat.messages.findIndex((m) => m.id === msgId);
  if (idx < 0) return;

  const reply = chat.messages[idx];
  reply.content = '';
  reply.reasoning = '';
  reply.error = false;
  reply.feedback = null;
  reply.streaming = true;
  chat.messages.length = idx + 1;

  renderChat();
  await runCompletion(chat, reply);
}

async function runCompletion(chat, reply) {
  const idx = chat.messages.indexOf(reply);
  const history = buildHistory(chat, idx);

  const controller = new AbortController();
  state.abort = controller;
  $('stopBtn').hidden = false;
  refreshSendState();
  setStatus('busy', 'Генерация…');

  const bubble = () => {
    const node = $('messages').querySelector(`.msg[data-id="${reply.id}"] .bubble`);
    return node;
  };

  let painted = 0;
  const repaint = (force) => {
    const now = Date.now();
    if (!force && now - painted < 55) return;
    painted = now;
    const b = bubble();
    if (b) { paintAssistant(b, reply); scrollToBottom(false); }
  };

  try {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      signal: controller.signal,
      body: JSON.stringify({
        model: state.model,
        messages: history,
        temperature: state.settings.temperature,
        max_tokens: state.settings.maxTokens,
        top_p: state.settings.topP,
        stream: true,
      }),
    });

    if (!res.ok && !(res.headers.get('content-type') || '').includes('text/event-stream')) {
      let message = `Сервер вернул ${res.status}`;
      try {
        const data = await res.json();
        if (data && data.error && data.error.message) message = data.error.message;
      } catch { /* keep the status-based message */ }
      throw new StreamError(message);
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    for (;;) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      const parts = buffer.split('\n\n');
      buffer = parts.pop() || '';

      for (const part of parts) {
        const line = part.split('\n').find((l) => l.startsWith('data:'));
        if (!line) continue;
        let evt;
        try { evt = JSON.parse(line.slice(5).trim()); } catch { continue; }

        if (evt.type === 'delta') { reply.content += evt.content; repaint(false); }
        else if (evt.type === 'reasoning') { reply.reasoning = (reply.reasoning || '') + evt.content; repaint(false); }
        else if (evt.type === 'error') { throw new StreamError(evt.message, evt.code); }
      }
    }

    reply.streaming = false;
    if (!reply.content.trim()) {
      reply.error = true;
      reply.content = 'Модель вернула пустой ответ. Попробуйте ещё раз или выберите другую модель.';
    }
    setStatus('', 'Online');
  } catch (err) {
    reply.streaming = false;
    if (err && err.name === 'AbortError') {
      if (!reply.content.trim()) {
        reply.error = true;
        reply.content = 'Генерация остановлена.';
      }
      setStatus('', 'Online');
    } else {
      const message = err instanceof StreamError
        ? err.message
        : 'Не удалось связаться с сервером. Проверьте интернет-соединение.';
      reply.error = true;
      reply.content = message;
      setStatus('err', 'Ошибка');
      toast('err', errorTitle(err), message, 8000);
    }
  } finally {
    state.abort = null;
    $('stopBtn').hidden = true;
    chat.updatedAt = Date.now();
    persistChats();
    renderSidebar();
    renderChat();
    refreshSendState();
  }
}

class StreamError extends Error {
  constructor(message, code) { super(message); this.name = 'StreamError'; this.code = code; }
}

function errorTitle(err) {
  const map = {
    bad_token: 'Проблема с токеном',
    no_token: 'HF_TOKEN не задан',
    model_unavailable: 'Модель недоступна',
    model_gated: 'Закрытая модель',
    model_loading: 'Модель загружается',
    rate_limit: 'Лимит запросов',
    timeout: 'Таймаут',
    network: 'Ошибка сети',
  };
  return (err && map[err.code]) || 'Ошибка';
}

function stopStream() {
  if (state.abort) { state.abort.abort(); state.abort = null; }
  $('stopBtn').hidden = true;
}

/* ==============================================================
   Model picker
   ============================================================== */
function openPicker() {
  state.modelFilter = '';
  $('modelSearch').value = '';
  renderPicker();
  openModal('modelModal');
}

function renderPicker() {
  renderStarters();
  const cats = $('pickerCats');
  const box = $('pickerModels');
  const q = state.modelFilter.trim().toLowerCase();
  const models = allModels();

  const groups = [{ id: 'all', name: 'Все модели', icon: '✨' }];
  for (const c of state.catalog.categories || []) groups.push({ id: c.id, name: c.name, icon: c.icon });
  if (state.customModels.length) groups.push({ id: 'custom', name: 'Мои модели', icon: '⭐' });

  cats.innerHTML = '';
  for (const g of groups) {
    const n = g.id === 'all' ? models.length : models.filter((m) => m.cat === g.id).length;
    const b = el('button', state.activeCat === g.id ? 'active' : '');
    b.innerHTML = `<span>${g.icon || ''}</span><span></span><span class="count">${n}</span>`;
    b.querySelectorAll('span')[1].textContent = g.name;
    b.onclick = () => { state.activeCat = g.id; renderPicker(); };
    cats.appendChild(b);
  }

  const visible = models.filter((m) => {
    const inCat = state.activeCat === 'all' || m.cat === state.activeCat;
    if (!q) return inCat;
    return (m.name + ' ' + m.id + ' ' + (m.desc || '')).toLowerCase().includes(q);
  });

  box.innerHTML = '';
  if (!visible.length) {
    box.appendChild(el('div', 'picker-empty',
      'Ничего не найдено. Добавьте модель по Model ID внизу окна.'));
    return;
  }

  for (const m of visible) {
    const card = el('div', 'model-card' + (m.id === state.model ? ' active' : ''));

    const body = el('div', 'mc-body');
    body.appendChild(el('div', 'mc-name', `${m.icon || ''} ${m.name}`.trim()));
    const id = el('div', 'mc-id', m.id); id.title = m.id;
    body.appendChild(id);
    if (m.desc) body.appendChild(el('div', 'mc-desc', m.desc));
    card.appendChild(body);

    if (m.custom) {
      const del = el('button', 'mc-del');
      del.innerHTML = ICON_TRASH;
      del.title = 'Убрать из списка';
      del.onclick = (e) => {
        e.stopPropagation();
        state.customModels = state.customModels.filter((x) => x.id !== m.id);
        save(LS.custom, state.customModels);
        renderPicker();
        toast('ok', 'Модель убрана', m.id, 2400);
      };
      card.appendChild(del);
    }

    const check = el('div', 'mc-check');
    check.innerHTML = '<svg viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5"/></svg>';
    card.appendChild(check);

    card.onclick = () => { setModel(m.id); closeModal('modelModal'); };
    box.appendChild(card);
  }
}

function setModel(id, opts = {}) {
  state.model = id;
  save(LS.model, id);
  const chat = currentChat();
  if (chat && opts.persistChat !== false) { chat.model = id; persistChats(); }
  updateTopbar();
  setStatus('', 'Online');
  if (!opts.silent) toast('ok', 'Модель выбрана', `${modelLabel(id)} — ${id}`, 2600);
}

function addCustomModel() {
  const input = $('customModel');
  const id = input.value.trim();
  if (!id) return;
  if (!/^[A-Za-z0-9._-]+\/[A-Za-z0-9._-]+(:[A-Za-z0-9._-]+)?$/.test(id)) {
    toast('err', 'Неверный Model ID', 'Формат: owner/name, например Qwen/Qwen2.5-7B-Instruct');
    return;
  }
  if (allModels().some((m) => m.id === id)) {
    toast('info', 'Эта модель уже в списке', id, 2600);
    setModel(id); closeModal('modelModal'); return;
  }
  state.customModels.push({ id, name: id.split('/').pop(), desc: 'Добавлена вручную' });
  save(LS.custom, state.customModels);
  input.value = '';
  state.activeCat = 'custom';
  renderPicker();
  setModel(id);
  closeModal('modelModal');
  toast('ok', 'Модель добавлена', id, 3000);
}

/* ==============================================================
   Settings
   ============================================================== */
function renderThemes() {
  const box = $('themePicker');
  if (!box) return;
  box.innerHTML = '';
  for (const t of THEMES) {
    const b = el('button', state.settings.theme === t.id ? 'active' : '');
    b.type = 'button';
    b.dataset.t = t.id;
    b.innerHTML = '<span class="t-swatch"></span><span></span>';
    b.querySelectorAll('span')[1].textContent = t.name;
    b.onclick = () => {
      state.settings.theme = t.id;
      save(LS.settings, state.settings);
      applyTheme(t.id);
      renderThemes();
      toast('ok', `Стиль: ${t.name}`, '', 1800);
    };
    box.appendChild(b);
  }
}

function fillSettings() {
  renderThemes();
  $('systemPrompt').value = state.settings.systemPrompt;
  $('temperature').value = state.settings.temperature;
  $('maxTokens').value = state.settings.maxTokens;
  $('topP').value = state.settings.topP;
  $('temperatureOut').textContent = Number(state.settings.temperature).toFixed(2);
  $('maxTokensOut').textContent = state.settings.maxTokens;
  $('topPOut').textContent = Number(state.settings.topP).toFixed(2);
}

function wireSettings() {
  $('systemPrompt').oninput = (e) => { state.settings.systemPrompt = e.target.value; save(LS.settings, state.settings); };
  const bind = (id, outId, key, fmt) => {
    $(id).oninput = (e) => {
      const v = Number(e.target.value);
      state.settings[key] = v;
      $(outId).textContent = fmt ? fmt(v) : v;
      save(LS.settings, state.settings);
    };
  };
  bind('temperature', 'temperatureOut', 'temperature', (v) => v.toFixed(2));
  bind('maxTokens', 'maxTokensOut', 'maxTokens');
  bind('topP', 'topPOut', 'topP', (v) => v.toFixed(2));

  $('resetSettings').onclick = () => {
    state.settings = { ...DEFAULT_SETTINGS };
    save(LS.settings, state.settings);
    applyTheme(state.settings.theme);
    fillSettings();
    toast('ok', 'Настройки сброшены', '', 2200);
  };
}

/* ==============================================================
   Modals / sidebar helpers
   ============================================================== */
function openModal(id) { $(id).hidden = false; }
function closeModal(id) { $(id).hidden = true; }
function openSidebar() { $('sidebar').classList.add('open'); $('scrim').hidden = false; }
function closeSidebar() { $('sidebar').classList.remove('open'); $('scrim').hidden = true; }

function autoGrow(ta) {
  ta.style.height = 'auto';
  ta.style.height = Math.min(ta.scrollHeight, 210) + 'px';
}
function refreshSendState() {
  $('sendBtn').disabled = !$('input').value.trim() || !!state.abort;
}

/* ==============================================================
   Boot
   ============================================================== */
async function loadCatalog() {
  try {
    const res = await fetch('/api/models');
    if (!res.ok) throw new Error(String(res.status));
    state.catalog = await res.json();
  } catch {
    toast('err', 'Каталог моделей недоступен', 'Не удалось загрузить список моделей с сервера.');
  }
  if (!state.model || !allModels().some((m) => m.id === state.model)) {
    state.model = state.model || state.catalog.default;
  }
  updateTopbar();
}

async function checkHealth() {
  try {
    const res = await fetch('/api/health');
    const data = await res.json();
    if (!data.token_configured) {
      setStatus('err', 'HF_TOKEN не задан');
      // The hint comes from the backend, so the hosted site and the browser
      // extension can each say where the token actually belongs.
      toast('err', 'HF_TOKEN не настроен',
        data.hint || 'Добавьте секрет HF_TOKEN в настройках хостинга и перезапустите приложение.',
        12000);
    } else {
      setStatus('', 'Online');
    }
  } catch {
    setStatus('err', 'Сервер недоступен');
  }
}

function wireEvents() {
  $('newChatBtn').onclick = () => { newChat(); closeSidebar(); };
  $('menuBtn').onclick = openSidebar;
  $('sidebarClose').onclick = closeSidebar;
  $('scrim').onclick = closeSidebar;

  $('searchInput').oninput = (e) => { state.filter = e.target.value; renderSidebar(); };

  $('settingsBtn').onclick = () => { fillSettings(); openModal('settingsModal'); closeSidebar(); };
  $('clearAllBtn').onclick = () => { clearAllChats(); closeSidebar(); };

  $('pickModelBtn').onclick = openPicker;
  $('modelSearch').oninput = (e) => { state.modelFilter = e.target.value; renderPicker(); };
  $('addModelBtn').onclick = addCustomModel;
  $('customModel').onkeydown = (e) => { if (e.key === 'Enter') addCustomModel(); };

  $('renameSave').onclick = commitRename;
  $('renameInput').onkeydown = (e) => { if (e.key === 'Enter') commitRename(); };

  document.querySelectorAll('[data-close]').forEach((b) => {
    b.onclick = () => { const m = b.closest('.modal'); if (m) m.hidden = true; };
  });
  document.querySelectorAll('.modal').forEach((m) => {
    m.addEventListener('click', (e) => { if (e.target === m) m.hidden = true; });
  });
  document.addEventListener('keydown', (e) => {
    if (e.key !== 'Escape') return;
    const open = [...document.querySelectorAll('.modal')].find((m) => !m.hidden);
    if (open) open.hidden = true;
    else closeSidebar();
  });

  const ta = $('input');
  ta.addEventListener('input', () => { autoGrow(ta); refreshSendState(); });
  ta.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); send(); }
  });
  $('sendBtn').onclick = () => send();
  $('stopBtn').onclick = stopStream;
}

async function boot() {
  wireEvents();
  wireSettings();
  fillSettings();
  renderSidebar();
  renderChat();
  refreshSendState();
  await Promise.all([loadCatalog(), loadFeatured()]);
  renderChat();
  renderStarters();
  checkHealth();
}

document.addEventListener('DOMContentLoaded', boot);
