# Session log

Append-only: дата + имя сессии + что сделано. Не перезаписывать.

## 2026-10-06 — regulus-implementation (devin-6d160aecdf7944d491a591dcff343d39, продолжение devin-ceb11c73667942fdac2fc12650cc3f6e)

Реализован герой Regulus по спеке docs/design/2026-10-05-regulus-design.md и плану docs/plans/2026-10-06-regulus-implementation.md. Task 1 (hero seam, третья раса, пассивы, суперпрыжок, тотем, ControlManager) — основная сессия; Task 2-6 — через dynamic workflow `wfr-310a3bbdf6014de39eb75eebb45a39a3` с суб-агентами: hearts+Lion's Heart, Debris Kick, Mania+Greed's Embrace+control sync, Counter+Evangelium/madness, позы/VFX/HUD. После каждого таска — ревьюер, в конце — 4 аудитора + интегратор фиксов (закрыт CRITICAL: ungated race choice → re-arm тотема). Гейты: `./gradlew build` зелёный, дифф-чек, все JSON валидны, runServer до Done. Версии: core 1.10.3→1.11.0, flight 1.6.7→1.7.0. In-game проверка за пользователем.

## 2026-10-07 — regulus-runtime-rework (devin-e34b96f7ee224b4ca4a2cb8565398bcd)

Переработаны Regulus input/animation/VFX поверх PR #3: ручные сердца через переназначаемую MMB с серверным выбором цели, Evangelium через удержание ПКМ без отмены от движения/урона, Debris Kick как восемь наземных срезов через HeroDestruction, поверхностное прицеливание Greed's Embrace и синхронизация точки, server-side готовность Counter. Убраны старые конусы/кольца и горизонтальные панели; добавлены camera-facing точки купола, красные owner-only маркеры носителей, разделение first-/third-person и рук книги, зеркальные позы и восстановление vanilla pivots. Перерисованы пять иконок и Evangelium, обновлены все 11 локалей, дизайн и справочник анимаций. Core 1.12.0, Flight без изменений относительно 1.7.0. Проверки: Java 17 `:viltrumitecore:test build --no-daemon` — 141 тест без failures/errors/skips, BUILD SUCCESSFUL; diff-check, JSON, 16×16 RGBA и ресурсы reobfuscated jar проверены. По прямому запрету пользователя игру, runClient/runServer, UI-тесты и субагентов не запускали. Runtime-внешний вид и управление после переработки не проверены.
