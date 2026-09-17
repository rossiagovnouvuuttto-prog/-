/**
 * Сборка промпта по профессиональной структуре:
 *
 *   Объект изображения + Стиль + Окружение + Камера + Освещение + Качество + Детализация
 *
 * Движок детерминированный и работает офлайн. Когда подключается настоящая
 * AI-модель, она заменяет только шаг «Объект изображения» и обогащение —
 * структура остаётся той же, поэтому результат всегда предсказуем.
 */

import { ENHANCERS, getOption } from '../data/options.js';
import { getStyle } from '../data/styles.js';
import { dedupeParts, pick, pickUnique, tidyPrompt } from '../utils.js';
import { translateToEnglish } from './translator.js';

/** Артикль и лёгкая «огранка» описания объекта. */
function normalizeSubject(subject) {
  let text = tidyPrompt(subject).replace(/^(a|an|the)\s+/i, '');
  if (!text) return 'an abstract composition';

  // Убираем висящие предлоги в конце.
  text = text.replace(/\b(in|on|with|of|at|from|and)$/i, '').trim();

  // Артикль определяется по первому слову: «futuristic city with flying cars»
  // остаётся единственным числом, несмотря на «cars» в конце.
  const first = text.split(/\s+/)[0].toLowerCase();
  const isPlural = /s$/.test(first) && !/(ss|us|is|ous)$/.test(first);
  const hasDeterminer = /^(a|an|the|two|three|several|many|no|his|her|their)$/.test(first);

  if (isPlural || hasDeterminer) return text;
  return `${/^[aeiou]/i.test(text) ? 'an' : 'a'} ${text}`;
}

/** Окружение: берём из описания, если оно там есть, иначе — типичное для стиля. */
function buildEnvironment(subjectEn, style, lightingOption) {
  const mentionsPlace = /\b(in|on|at|inside|under|above|near|among|through|street|city|forest|room|space|sky|beach|mountain|desert|ocean|studio|background)\b/i
    .test(subjectEn);

  if (mentionsPlace) {
    // Описание уже содержит место — добавляем только атмосферу стиля.
    return pick(style.environments).replace(/^(a|an|the)\s+/i, '');
  }

  const base = pick(style.environments);
  if (lightingOption.id === 'neon' && style.id !== 'cyberpunk') {
    return `${base}, glowing neon accents`;
  }
  if (lightingOption.id === 'sunset') {
    return `${base}, warm evening sky`;
  }
  return base;
}

/**
 * Основная функция сборки.
 *
 * @param {object} input
 * @param {string} input.idea     — описание пользователя (рус/англ)
 * @param {object} input.settings — { style, camera, lighting, quality, aspect }
 * @param {object} [input.overrides] — точечная замена частей (для вариантов)
 * @returns {{prompt: string, parts: object, negative: string, ratio: string}}
 */
export function buildPrompt({ idea, settings, overrides = {} }) {
  const style = getStyle(settings.style);
  const camera = getOption('camera', settings.camera);
  const lighting = getOption('lighting', settings.lighting);
  const quality = getOption('quality', settings.quality);
  const aspect = getOption('aspect', settings.aspect);

  const subjectEn = overrides.subject || normalizeSubject(translateToEnglish(idea));
  const environment = overrides.environment || buildEnvironment(subjectEn, style, lighting);

  // Структура промпта: объект → стиль → окружение → камера → свет → качество → детали.
  const sections = [
    ['subject',     [subjectEn]],
    ['style',       style.modifiers.slice(0, overrides.styleDepth ?? 2)],
    ['environment', [environment]],
    ['camera',      [camera.prompt]],
    ['lighting',    overrides.lighting ? [overrides.lighting] : [lighting.prompt, style.lighting]],
    ['quality',     [quality.prompt]],
    ['detail',      overrides.detail || style.detail],
    ['format',      [aspect.prompt]],
  ];

  // Дедупликация идёт по отдельным фрагментам, иначе «neon lights»
  // из настроек и из стиля попадают в промпт дважды.
  const seen = new Set();
  const lines = [];
  const parts = {};

  for (const [key, fragments] of sections) {
    const kept = dedupeParts(fragments.flatMap((fragment) => String(fragment).split(/,\s*/)))
      .filter((fragment) => {
        const token = fragment.toLowerCase();
        if (seen.has(token)) return false;
        seen.add(token);
        return true;
      });

    if (!kept.length) continue;
    parts[key] = kept.join(', ');
    lines.push(parts[key]);
  }

  return {
    prompt: tidyPrompt(lines.join(', ')),
    multiline: lines.join(',\n'),
    parts,
    negative: style.negative,
    ratio: aspect.ratio,
  };
}

/**
 * Улучшение промпта: добавляем композицию, цвет, атмосферу и финальный акцент,
 * не ломая исходную структуру и не повторяя уже сказанное.
 */
export function enhancePrompt(prompt, multiline = '') {
  const existing = prompt.toLowerCase();
  const additions = [];

  for (const group of Object.values(ENHANCERS)) {
    const candidate = group.find((item) => !existing.includes(item.toLowerCase().slice(0, 14)));
    if (candidate) additions.push(candidate);
  }

  // Структура исходного промпта сохраняется: дополнения идут отдельной строкой.
  const baseLines = (multiline || prompt).split('\n').map((line) => line.replace(/,\s*$/, ''));
  const seen = new Set(baseLines.flatMap((line) => line.split(/,\s*/)).map((part) => part.toLowerCase()));
  const fresh = dedupeParts(additions).filter((item) => !seen.has(item.toLowerCase()));
  const lines = fresh.length ? [...baseLines, fresh.join(', ')] : baseLines;

  return {
    prompt: tidyPrompt(lines.join(', ')),
    multiline: lines.join(',\n'),
    added: fresh,
  };
}

/** Наборы настроений для вариантов промпта. */
const VARIANT_MOODS = [
  { name: 'Драматичный', lighting: 'dramatic backlight, deep shadows', detail: ['high contrast', 'moody atmosphere'] },
  { name: 'Мягкий',      lighting: 'soft diffused light, gentle highlights', detail: ['delicate textures', 'pastel tones'] },
  { name: 'Эпичный',     lighting: 'volumetric god rays, glowing atmosphere', detail: ['epic scale', 'sweeping perspective'] },
  { name: 'Минимализм',  lighting: 'clean even lighting, subtle gradient', detail: ['minimalist composition', 'negative space'] },
  { name: 'Ночной',      lighting: 'moonlight with cool blue tones', detail: ['night ambience', 'glowing highlights'] },
];

/**
 * Несколько вариантов одного промпта: меняются окружение, свет и детализация,
 * объект остаётся прежним.
 */
export function buildVariants({ idea, settings, count = 3 }) {
  const style = getStyle(settings.style);
  const usedEnvironments = new Set();
  const total = Math.min(count, VARIANT_MOODS.length);

  return Array.from({ length: total }, (_, index) => {
    const mood = VARIANT_MOODS[index % VARIANT_MOODS.length];
    const built = buildPrompt({
      idea,
      settings,
      overrides: {
        environment: pickUnique(style.environments, usedEnvironments),
        lighting: mood.lighting,
        detail: [...mood.detail, ...style.detail.slice(0, 1)],
        styleDepth: 3,
      },
    });

    return { id: `${mood.name}-${index}`, name: mood.name, ...built };
  });
}
