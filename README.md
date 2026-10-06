# Viltrumite Mods (Forge 1.20.1)

Два связанных мода в одном репозитории:

| Папка | Мод | Версия | Что это |
|---|---|---|---|
| `viltrumitecore/` | ViltrumiteForge (`viltrumitecore`) | 1.11.0 | основной мод: способности, анимации, VFX, предметы, мир-ивенты |
| `viltrumiteflight/` | ViltrumiteFlight (`viltrumiteflight`) | 1.7.0 | полёт и его визуал; **core требует этот мод** (`viltrumiteflight >= 1.6.6`) |

Minecraft 1.20.1, Forge 47.x, Java 17, маппинги `official` (Mojang).

## Откуда исходники

Оригинального репозитория не было, поэтому код восстановлен из релизных jar (лежат в `original-jars/`):
декомпиляция Vineflower + перевод SRG-имён Minecraft (`m_109156_` и т.п.) в официальные имена Mojang.
Комментарии и часть имён локальных переменных потеряны. Подробности и то, что требует ручной правки, — в `DECOMPILE_REPORT.md`.

## Сборка

```bash
./gradlew build
```

Готовые jar: `viltrumitecore/build/libs/` и `viltrumiteflight/build/libs/`. Ставить в игру нужно оба.

## Запуск в dev

```bash
./gradlew :viltrumitecore:runClient     # оба мода сразу
./gradlew :viltrumiteflight:runClient   # только flight
```

## IDE

IntelliJ IDEA → Open → папка репозитория (импортируется как Gradle-проект). Первый импорт качает Minecraft и Forge, это долго.

## Версии

Версия мода задаётся в `build.gradle` подпроекта (`version = '...'`) и сама подставляется в `mods.toml`.

## CI (GitHub Actions)

- **Build** — собирает на каждый пуш, jar-ы в артефактах запуска.
- **Decompile (one-shot)** — уже отработал при создании репо. Повторно перезапишет исходники, только если запустить с `force = 1`.