/**
 * Настройки промпта: камера, освещение, качество, формат.
 * `prompt` — точная английская формулировка, которая попадает в результат.
 */

export const SETTING_GROUPS = [
  {
    id: 'camera',
    label: 'Камера',
    icon: 'camera',
    options: [
      { id: 'dslr',      label: 'DSLR',       prompt: 'DSLR photography, 50mm lens, shallow depth of field' },
      { id: 'cinematic', label: 'Cinematic',  prompt: 'cinematic camera angle, anamorphic wide shot' },
      { id: 'drone',     label: 'Drone shot', prompt: 'drone shot, aerial perspective from above' },
      { id: 'macro',     label: 'Macro',      prompt: 'macro shot, extreme close-up, fine micro detail' },
    ],
  },
  {
    id: 'lighting',
    label: 'Освещение',
    icon: 'sun',
    options: [
      { id: 'sunset', label: 'Sunset',          prompt: 'golden hour sunset light, warm rim glow' },
      { id: 'neon',   label: 'Neon lights',     prompt: 'neon lights, volumetric lighting, colored reflections' },
      { id: 'studio', label: 'Studio lighting', prompt: 'professional studio lighting, soft shadows' },
    ],
  },
  {
    id: 'quality',
    label: 'Качество',
    icon: 'sparkles',
    options: [
      { id: 'hd', label: 'HD', prompt: 'high definition, HD quality' },
      { id: '4k', label: '4K', prompt: '4K resolution, ultra sharp' },
      { id: '8k', label: '8K', prompt: '8K detail, ultra high resolution' },
    ],
  },
  {
    id: 'aspect',
    label: 'Формат',
    icon: 'frame',
    options: [
      { id: '16:9', label: '16:9', prompt: 'wide 16:9 composition', ratio: '--ar 16:9' },
      { id: '1:1',  label: '1:1',  prompt: 'square 1:1 composition', ratio: '--ar 1:1' },
      { id: '9:16', label: '9:16', prompt: 'vertical 9:16 composition', ratio: '--ar 9:16' },
    ],
  },
];

export const GROUP_MAP = Object.fromEntries(SETTING_GROUPS.map((group) => [group.id, group]));

export function getOption(groupId, optionId) {
  const group = GROUP_MAP[groupId];
  if (!group) return null;
  return group.options.find((option) => option.id === optionId) || group.options[0];
}

/** Дополнительные формулировки для кнопки «Улучшить промпт». */
export const ENHANCERS = {
  composition: [
    'rule of thirds composition',
    'balanced composition with strong focal point',
    'dynamic perspective with leading lines',
    'symmetrical framing',
  ],
  color: [
    'cinematic color grading, teal and orange palette',
    'harmonious color palette with deep contrast',
    'rich vibrant colors with soft gradients',
    'muted cinematic tones',
  ],
  atmosphere: [
    'atmospheric haze and depth',
    'subtle particles floating in the air',
    'soft ambient fog in the background',
    'dramatic mood with layered depth',
  ],
  finish: [
    'award winning photography',
    'masterpiece quality, highly detailed',
    'trending on ArtStation',
    'professional grade render',
  ],
};

/** Идеи для кнопки «Случайная идея». */
export const IDEA_SEEDS = [
  'футуристический город с летающими машинами',
  'девушка-самурай под дождём из лепестков сакуры',
  'старый маяк на скале во время шторма',
  'кот-астронавт в открытом космосе',
  'заброшенный поезд, заросший цветами',
  'дракон спит на горе золота в пещере',
  'уютная кофейня зимним вечером',
  'робот поливает сад на крыше небоскрёба',
  'портрет пожилого рыбака с обветренным лицом',
  'подводный город со светящимися куполами',
  'библиотека с бесконечными лестницами',
  'мотоциклист на пустынном шоссе на закате',
];
