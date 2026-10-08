# Session log

Append-only: дата + имя сессии + что сделано. Не перезаписывать.

## 2026-10-06 — regulus-implementation (devin-6d160aecdf7944d491a591dcff343d39, продолжение devin-ceb11c73667942fdac2fc12650cc3f6e)

Реализован герой Regulus по спеке docs/design/2026-10-05-regulus-design.md и плану docs/plans/2026-10-06-regulus-implementation.md. Task 1 (hero seam, третья раса, пассивы, суперпрыжок, тотем, ControlManager) — основная сессия; Task 2-6 — через dynamic workflow `wfr-310a3bbdf6014de39eb75eebb45a39a3` с суб-агентами: hearts+Lion's Heart, Debris Kick, Mania+Greed's Embrace+control sync, Counter+Evangelium/madness, позы/VFX/HUD. После каждого таска — ревьюер, в конце — 4 аудитора + интегратор фиксов (закрыт CRITICAL: ungated race choice → re-arm тотема). Гейты: `./gradlew build` зелёный, дифф-чек, все JSON валидны, runServer до Done. Версии: core 1.10.3→1.11.0, flight 1.6.7→1.7.0. In-game проверка за пользователем.

## 2026-10-07 — regulus-runtime-rework (devin-e34b96f7ee224b4ca4a2cb8565398bcd)

Переработаны Regulus input/animation/VFX поверх PR #3: ручные сердца через переназначаемую MMB с серверным выбором цели, Evangelium через удержание ПКМ без отмены от движения/урона, Debris Kick как восемь наземных срезов через HeroDestruction, поверхностное прицеливание Greed's Embrace и синхронизация точки, server-side готовность Counter. Убраны старые конусы/кольца и горизонтальные панели; добавлены camera-facing точки купола, красные owner-only маркеры носителей, разделение first-/third-person и рук книги, зеркальные позы и восстановление vanilla pivots. Перерисованы пять иконок и Evangelium, обновлены все 11 локалей, дизайн и справочник анимаций. Core 1.12.0, Flight без изменений относительно 1.7.0. Проверки: Java 17 `:viltrumitecore:test build --no-daemon` — 141 тест без failures/errors/skips, BUILD SUCCESSFUL; diff-check, JSON, 16×16 RGBA и ресурсы reobfuscated jar проверены. По прямому запрету пользователя игру, runClient/runServer, UI-тесты и субагентов не запускали. Runtime-внешний вид и управление после переработки не проверены.

## 2026-10-07 — regulus-review-fixes (devin-deb6106cfc5e442cb817e966487cf846)

Фиксы по итогам ревью devin-e34b96f7ee224b4ca4a2cb8565398bcd (4 суб-агента). Major: волна Debris Kick ищет поверхность ±2 от текущей высоты стояния (проходит ступеньки в 2 блока, умирает на отвесных стенах 3+), обход vanilla i-frames восстановлен как в baseline (claim только после успешного попадания); каст без земли под ногами отменяется с фидбеком и без кулдауна; центральная колонна и lane-цикл уважают HeroDestruction.canDestroy/mobGriefing и результат destroyBlock; stale-кэш surfaces убран. Madness: ATTACK_DAMAGE 0.4 через MULTIPLY_TOTAL (+40%), эффекты безумия видимые, таймер на HUD. Сердца назначаются клавишей N (ability_6) вместо MMB — конфликта с pick-item нет; носители подсвечены красным силуэтом сквозь блоки через OutlineBufferSource (owner-only), вместо точки на груди. Пять иконок освежены: ярче, на цветном фоне в стиле punch/barrage. Убран мёртвый код: maxHitLoss/recordAppliedLoss/RITUAL_INTERRUPT_DAMAGE/ritualDamageInterrupts, dead-guard в GreedsEmbrace.start, RITUAL-политика внутри Lion, toggleCarrier снимает невалидного носителя, spectator не биндит сердца, смерть во время ритуала не списывает кулдаун. Тесты обновлены, 142 зелёные. In-game проверка за пользователем.

## 2026-10-07 — Regulus animation rewrite (Devin)
- d1546d0: per-ability Model/RendererCore/FirstPerson mixins for Regulus, RegulusActionClock (synced ticks, no snapshot extrapolation), weight layers; old RegulusModelMixin/FirstPersonRegulusMixin removed.
- runClient smoke via ffmpeg x11grab storyboards: Lion, Kick, Embrace, Ritual(+cancel), Madness walk, Mania FP. Unchecked: Counter, Mania 3rd person, shaders, old Viltrumite anim regression.
- Gotcha: xdotool `key` is too short for ability KeyMappings; use keydown, sleep 0.15, keyup. Evangelium needs RMB held (mousedown 3) the whole ritual.

## 2026-10-08 — hero-toolkit + скилл add_hero (Notion AI)
- Из Regulus вынесен общий набор механик для всех героев: `HeroFx`/`HeroFxS2CPacket`/`HeroImpactFx` (осколки, ударные волны, след клинка, взлёт, вспышка), `CameraShake`, `PixelVfx`, `HeroDebris` (вырывание земли, летящие блоки; Thunderclap переведён на него), `HeroShockwave` (удар при приземлении), `HeroSuperJump` + `SuperJumpClient` (суперпрыжок для любого героя через `superJumpVelocity`). В `HeroDefinition` новые хуки `cancelsFallDamage`, `onLanded`, `superJumpVelocity`, `onSuperJump`. Поведение Regulus должно остаться прежним; подпись клавиши — «Super Jump» во всех локалях, id клавиши не менялся.
- Добавлен скилл `.agents/skills/add_hero/` (процедура переноса героя, справка по точкам интеграции и общему набору, шаблон дизайн-документа, скрипт `check_hero_resources.py` для lang/звуков/текстур/миксинов; 6 известных старых проблем в baseline).
- Проверки: `./gradlew build` зелёный, тесты зелёные. В игре не проверялось.

## 2026-10-08 — homelander (Notion AI)
- Хоумлендер заменил Вильтрумита: старые сохранения и флаг `IS_VILTRUMITE` переходят в `HOMELANDER`. Кит: полёт, удар, хлопок, приземление, рывок только в полёте; лазеры из глаз (урон 1,5 каждые 4 тика ≈ 3,75 сердца/с, поджог, следы гари без ломания блоков, перегрев глаз), фокус (до 8 ближайших целей сквозь стены своими цветами, HP, реальный маршрут A*, громкие шаги, приглушение остального, страх у целей), рык (конус 60°/12 блоков, отбрасывание, Slowness II, кд 15 с), регенерация. Скин и звуки из Codex, 6 иконок нарисованы (64×64).
- Seam: `LegacyKit` + `allowsLegacyAbility`, `heroInputSlots/heroActionFor`, `onHurt`, `onDimensionChange`, `abilityIcon`, общие поля снапшота `resource/resourceLocked/heroFlags`, `NoHeroBranchTest`. Core 1.13.0 → 1.14.0.
- Команда `/viltrumite choose [игрок]` (только оператор) снова открывает окно выбора героя.
- Фикс 1.14.1: старые слоты Вильтрумита (захват, блок и т.д.) при входе/выборе сбрасываются на набор Хоумлендера (`HeroRegistry.repairLoadout`); Хоумлендер получает +10 сердец и +10 брони.
- 1.14.2: лазер без доводки (точный хитбокс), ожоги в точке попадания (1/16 блока) непрерывной линией при ведении луча; рык 90° + всё в 3 блоках кроме спины, по ближайшей к лучу точке хитбокса, урон 3–6; пути фокуса — плавный светящийся поток (Chaikin по A*, мягкая лента в 3 слоя, колыхание, импульсы к цели).
- 1.14.4: газ путей чуть плотнее и насыщеннее.
- 1.14.3: пути фокуса — мягкий газ (обычное смешивание, приглушённый цвет, ширина ~2 блока, медленные клубы), маршрут пересчитывается редко (не раньше 4 с и только при сдвиге конца >6 блоков, иначе раз в 15 с), упрощение Douglas-Peucker + 4× Chaikin, плавная смена старого пути на новый за 1,5 с, концы следуют за игроком и целью вживую.
- Проверки: `./gradlew build` и тесты зелёные, `runServer` до Done, checker 0 новых проблем. В игре (клиент) не проверялось.
