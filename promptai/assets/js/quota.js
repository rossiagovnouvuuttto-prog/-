/** Бесплатный лимит генераций: сколько осталось и можно ли генерировать. */

import { auth, currentPlan } from './auth.js';
import { store } from './store.js';

/** Расход считается отдельно для гостя и для каждого аккаунта. */
function scope() {
  return auth.current()?.id || 'guest';
}

export function quota() {
  const plan = currentPlan();
  const { used } = store.getUsage(scope());
  const limit = plan.dailyLimit;
  const unlimited = limit === Infinity;

  return {
    plan,
    used,
    limit,
    unlimited,
    left: unlimited ? Infinity : Math.max(0, limit - used),
    exhausted: !unlimited && used >= limit,
    ratio: unlimited ? 0 : Math.min(1, used / limit),
  };
}

/** Списать одну генерацию. Возвращает false, если лимит уже исчерпан. */
export function spendGeneration() {
  const state = quota();
  if (state.exhausted) return false;
  if (!state.unlimited) store.bumpUsage(scope(), 1);
  return true;
}

/** Сколько вариантов промпта доступно на текущем тарифе. */
export function maxVariants() {
  return currentPlan().variants;
}
