# ABOBUS123 — контекст порта на Minecraft 1.21.11 Fabric

## Промпт для начала работы

> Продолжаю портирование моего клиентского мода ABOBUS123 с Minecraft 1.18.2
> на 1.21.11 Fabric. Порт компиляции ЗАВЕРШЁН — было 951 ошибка, стало 0,
> JAR собирается. Осталась доводка миксинов по логам запуска игры.
>
> Проект в репозитории. Сборка идёт через GitHub Actions (файл
> .github/workflows/build.yml), потому что локально скачать Minecraft
> с Maven может не получиться.
>
> Читай ABOBUS123-CONTEXT.md — там версии, инструменты и список
> уже потерянных функций. Не удаляй функции без причины и не откатывай
> defaultRequire: 1 в reallyvisuals.mixins.json.

---

## Версии (проверены по jar'у, не угаданы)

| Компонент | Значение |
|---|---|
| Minecraft | 1.21.11 |
| Yarn | 1.21.11+build.4 |
| Fabric Loader | 0.18.1 |
| Fabric API | 0.141.6+1.21.11 |
| Loom | 1.14 |
| Gradle | 9.7.1 |
| Java | 21 |

## КРИТИЧНО

`reallyvisuals.mixins.json` → `injectors.defaultRequire = 1`.

С нулём несовпавшие инъекции отключаются МОЛЧА. За время порта так
нашлось больше десяти миксинов, целившихся в удалённые методы, —
компилятор их не видит в принципе, только рантайм.

## Инструменты в build.gradle

Задачи, написанные под этот порт. Спрашивают живой замапленный jar,
а не догадки:

```
./gradlew sig2   -Pcls=<класс> [-Pm=<фильтр>]   # сигнатуры методов
./gradlew dump   -Pcls=<класс> [-Pm=<фильтр>]   # + поля и родительские классы
./gradlew findCls -Pq=<подстрока>               # где лежит класс
./gradlew findRet -Pt=<тип>                     # кто возвращает тип
./gradlew auditMixins                           # сверить ВСЕ инъекции разом
./gradlew auditMembers                          # сверить @Shadow/@Accessor/@Invoker
./gradlew auditSigs                             # сверить ПАРАМЕТРЫ обработчиков
```

Три аудита ловят три разных класса ошибок, и все три невидимы компилятору:

| Задача | Что ловит | Как проявлялось |
|---|---|---|
| `auditMixins` | целевого метода нет | `Cannot remap` при сборке |
| `auditMembers` | `@Shadow`/`@Accessor` на удалённый член | `InGameHud.scaledWidth` |
| `auditSigs` | параметры обработчика не те | `InvalidInjectionException` в рантайме |

`auditMembers` нужен отдельно: `@Shadow` на несуществующее поле
компилируется молча и падает только в рантайме. Так и проскочил
`InGameHud.scaledWidth`. На текущем коде — 0 отсутствующих.

Если `maven.fabricmc.net` недоступен локально, всё это гоняется в CI:
`.github/workflows/audit.yml` (вкладка Actions → audit). Там же лежит
готовый список классов для `dump`.

Проект лежит в подкаталоге `ABOBUS123-1.21.11/`, поэтому оба workflow
используют `working-directory`.

`auditMixins` — главный. Показывает по каждому миксину, существует ли
целевой класс и метод. Экономит цикл «сборка → запуск → краш».

## Новые файлы порта

- `gui/render/Buf.java` — прослойка вместо удалённого immediate-mode
  (Tessellator + RenderSystem.setShader). Держит старый API поверх
  VertexConsumerProvider + RenderLayers.
- `WorldRenderHandler.java` — WorldRenderEvents.AFTER_ENTITIES вместо
  инъекции в WorldRenderer.render.
- `assets/really/textures/gui/disc.png` — сглаженный диск; его квадранты
  дают скруглённые углы вместо удалённого SDF-шейдера.

## Ключевые замены API

| Было (1.18.2) | Стало (1.21.11) |
|---|---|
| Tessellator + setShader | Buf → VertexConsumerProvider + RenderLayers |
| MatrixStack в GUI | DrawContext + Matrix3x2fStack (int-координаты!) |
| RenderLayer.getLines() | RenderLayers.LINES |
| new Identifier() | Identifier.of() |
| Vec3f | org.joml + RotationAxis |
| mouseClicked(x,y,btn) | mouseClicked(Click, boolean) |
| keyPressed(k,s,m) | keyPressed(KeyInput) |
| onMouseButton(w,b,a,m) | onMouseButton(w, MouseInput, a) |
| поля GameOptions | SimpleOption + геттеры |
| NBT шалкера | DataComponentTypes.CONTAINER |
| getSkinTexture() | getSkin().body().texturePath() |
| GameProfile.getName() | .name() |
| entity.getPos() | getEntityWorld() / getEntityPos() |
| isFallFlying() | isGliding() |
| isPermanent() | isInfinite() |
| PositionedSoundInstance.master | .ui() |
| TextRenderer.drawWithShadow | context.drawTextWithShadow (int!) |

## Потерянные функции (цели удалены в 1.21.11)

| Функция | Причина |
|---|---|
| Скайбокс и цвет неба CustomWorld | renderSky, getSkyColor удалены |
| Дальность тумана + NoFluid | setShaderFogStart/End убраны |
| Модель биты SwordBat | BakedModel удалён |
| Режим «Свеня» в TargetESP | нужен OrderedRenderCommandQueue |
| Подписи путевых точек | в мире нет DrawContext (луч маяка работает) |
| Строки клиента в F3 | DebugHud.getLeftText удалён |
| Подсветка предметов на земле | ItemEntityRenderer.renderLayer удалён |
| Анимация чанков | WorldRenderer.render переписан |
| Sodium-миксины | целятся в Sodium 0.4.x, классов нет |

Возвращаемы при желании: подписи точек (проекция на экран в HUD-проходе),
«Свеня» (протянуть commandQueue из WorldRenderEvents).

## Изменения поведения (компилятор не поймает)

- Скругления: текстура диска вместо SDF — на больших радиусах мягче
- Кольца: сегменты вместо стенсила — возможна ступенчатость
- Голова цели в TargetHud: только у игроков, мобы → запасной квадрат
- Анимация чата: общий сдвиг блока вместо построчного
- PerformanceBoost при выключении вернёт графику на FANCY

## Проход по миксинам (сверено с замапленным jar через CI)

Несовпадений remap было 12 → стало 4. Оставшиеся 4 сидят в миксинах,
которые не подключены ни к одному конфигу, то есть в рантайм не идут.

### Починено

| Было | Стало | Что вернулось |
|---|---|---|
| `InGameHud.scaledWidth/scaledHeight` | полей нет → `DrawContext.getScaledWindowWidth/Height()` | HUD, скорборд, ClickPearl |
| `GameRenderer.bobViewWhenHurt` | `tiltViewWhenHurt` | твик «Тряска урона» |
| `ClientWorld.playSound(...FFZ)` | `(...FFZJ)` — добавился `long seed` | Hit Sounds: снятие ванильного крита |
| `PlayerEntityRenderer.getTexture(Entity)` | `getTexture(PlayerEntityRenderState)` | Player Skins |
| `PlayerEntityRenderer.scale(Entity,…,float)` | `scale(PlayerEntityRenderState, MatrixStack)` | морф Player Skins |
| `PlayerEntityRenderer.render` (метода нет) | `ChinaHat.render3D` из `WorldRenderHandler` | China Hat (локальный игрок) |
| `World.getTime` | метода нет; `getTimeOfDay` уже перехвачен | Time Changer не пострадал |
| `Screen.renderTooltip` | метода нет; `HandledScreen.drawMouseoverTooltip` жив | Shulker Preview не пострадал |

Снят `require = 0` со всех инъекций, чьи цели подтверждены аудитом, —
теперь они падают громко, а не отключаются молча. Это тот же принцип,
что и `defaultRequire: 1`, только на уровне отдельной инъекции.

### Осталось потерянным (цели правда удалены)

`DebugHud.getLeftText`, `WorldRenderer.renderLayer` ×2,
`ChunkBuilder$BuiltChunk.setOrigin` — строки F3 и анимация чанков,
уже были в списке выше.

### Миксины-сироты (файл есть, в конфиге нет → не грузятся)

`EntityRenderDispatcherMixin`, `ClientWorldPropertiesMixin`,
`BuiltChunkMixin`, `LivingEntityMixin`, `ItemEntityRendererMixin`,
`DebugHudMixin`, `accessor.WorldRendererAccessor`.
Оставлены как заготовки для восстановления; кода они сейчас не меняют.

`reallyvisuals.sodium.mixins.json` и `reallyvisuals.vanillachunks.mixins.json`
не перечислены в `fabric.mod.json`, поэтому тоже не грузятся.

### Выпотрошены до пустых классов при порте (в списке потерь их не было)

`ArmorFeatureRendererMixin`, `FeatureRendererMixin`, `OverlayTextureMixin` —
остались `public class X {}` без единой инъекции. Что именно они делали
в 1.18.2, по текущему исходнику не восстановить: нужен старый проект.

## Пройден лог запуска (FCL, Android, 30.08)

Игра падала на старте, один краш:

```
InjectionError: Redirector redirectModelRender ... LivingEntityRendererMixin
failed injection check, (0/1) succeeded. Scanned 0 target(s).
```

| Миксин | Причина | Чем заменено |
|---|---|---|
| `LivingEntityRendererMixin` | `@Redirect` на `Model.render` — вызова больше нет, всё через `OrderedRenderCommandQueue` | `@Inject` в `getMixColor(LivingEntityRenderState)`, флаг `state.hurt`; альфа ванилы сохранена, меняется только оттенок |
| `GameRendererMixin` | `@Redirect` на `net.minecraft.util.math.Matrix4f` — класс удалён в 1.19; следующий краш в очереди | `@Inject` в `getBasicProjectionMatrix`, правка `m00 = m11 / ratio` прямо в матрице |

`hasLabel` прибит к дескриптору `(Lnet/minecraft/entity/LivingEntity;D)Z` —
по одному имени он ловил ещё и мостовой перегруз с `Entity`.

Модули **Hit Color** и **Aspect Ratio** таким образом сохранены, а не потеряны.

## Второй лог (20:00) — ещё два краша

`LivingEntityRenderer` применился, краш переехал дальше:

```
Invalid descriptor on GameRendererMixin->@Inject::onRenderHead
Expected (Lnet/minecraft/class_9779;Z...)V but found (FJZ...)V
```

| Миксин | Было | Стало |
|---|---|---|
| `GameRendererMixin.onRenderHead` | `(float, long, boolean)` | `render(RenderTickCounter, boolean)` |
| `CameraMixin.applyFreeLook` | `BlockView` первым параметром | `update(World, …)` — был под `require = 0`, то есть Free Look не работал вообще |
| `WorldRendererMixin.onSetupFrustum` | только `CallbackInfoReturnable` | `+ (Matrix4f, Matrix4f, Vec3d)` — тоже заглушен, отсечение по фрустуму не питалось |

`Camera` и `WorldRenderer` нашлись не по крашу, а задачей `auditSigs`,
написанной после второго лога.

## Третий лог (20:29) — краш в оверлеях

```
Invalid descriptor on InGameOverlayRendererMixin->@Inject::abobus123$onRenderFireOverlay
Expected (Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;Lnet/minecraft/class_1058;...)V
found    (Lnet/minecraft/class_310;Lnet/minecraft/class_4587;...)V
```

Оба обработчика брали `(MinecraftClient, MatrixStack)`. В 1.21.11 сюда
приходят спрайт и `VertexConsumerProvider`, причём **порядок у двух
методов разный**:

```
renderFireOverlay  (MatrixStack, VertexConsumerProvider, Sprite)
renderInWallOverlay(Sprite, MatrixStack, VertexConsumerProvider)
```

Вернулись твик «Оверлей огня» и модуль No Fluid.

### Дыра в самом auditSigs

Этот краш `auditSigs` обязан был поймать — и отчитался нулём.
Его regex обработчика не учитывал модификаторы, поэтому
`private static void foo(` не совпадал, и обе инъекции **пропускались
молча**. Проверка, которая тихо ничего не проверяет, хуже отсутствующей.

Исправлено: модификаторы разбираются, неразобранный обработчик теперь
считается ошибкой и печатается, а при несовпадении выводятся **полные**
имена типов — чтобы пакет для замены брался из jar'а, а не по памяти.

Остальные предупреждения третьего лога к моду не относятся: Sodium/Iris,
отсутствие udev и `/proc/stat` на Android, офлайн-авторизация 401,
проверка Patreon у Xaero.

`@ModifyVariable` в `TextRendererMixin` под `auditSigs` не попадает
(другие правила сигнатур) — проверен вручную, дескрипторы `draw(...)`
совпадают.

## Ревизия: молча не загружавшиеся миксины

Два миксина существовали, компилировались и были полностью рабочими, но
**не значились ни в одном конфиге**, поэтому не грузились никогда.
Модули при этом видны в меню и включаются — просто без эффекта.

| Миксин | Что было мертво | Как починено |
|---|---|---|
| `LivingEntityMixin` | No Jump Delay, Jump Circles, твик «Чёрные сердца» | добавлен в конфиг; `hasStatusEffect` теперь берёт `RegistryEntry`, а не `StatusEffect` |
| `EntityRenderDispatcherMixin` | Hide AC Bot, отсечение мелких сущностей у FPS Boost | переписан на `shouldRender(Entity, Frustum, …)` и добавлен в конфиг |

`EntityRenderDispatcherMixin` целился в `render()`, который в 1.21.11 видит
только `EntityRenderState` и до сущности не достаёт. Но `EntityRenderManager`
по-прежнему объявляет

```
boolean shouldRender(Entity entity, Frustum frustum, double x, double y, double z)
```

— он принимает саму сущность и решает, рисовать ли её вообще, что подходит
лучше, чем отмена `render()`. Ванильный frustum-culling там уже есть, поэтому
своим остался только отсев по расстоянию.

Проверка, которая это нашла: сопоставление модулей из `ModuleManager` с
файлами, реально попадающими в загружаемые конфиги. Пункт «Отсечение
сущностей» из таблицы потерь выше снят.

## Состояние аудитов

```
auditMixins   — чисто (остались только миксины-сироты, они не грузятся)
auditMembers  — 0 отсутствующих членов
auditSigs     — 0 несовпадений сигнатур
auditOverrides — 0 мёртвых переопределений
```

`auditOverrides` добавлен отдельно: метод, чья сигнатура разошлась с
родительской, становится новым методом, который игра не вызывает никогда.
`javac` ловит это только при `@Override`, а его в коде почти нигде не было.

`require = 0` в загружаемых миксинах больше нет ни одного: все цели,
члены и сигнатуры сверены с jar'ом, поэтому `defaultRequire: 1`
действует на весь конфиг и дрейф падает громко.

## Что осталось

Перезапустить игру и пройти лог заново. China Hat теперь рисуется только на своём
игроке — на чужих нужен `OrderedRenderCommandQueue`, тот же блокер,
что и у режима «Свеня».
