# FireBow (Огненный лук)

Плагин для Paper 1.21.x, добавляющий новый предмет **«Огненный лук»**, который стреляет кастомными снарядами **«Метеоритами»**. Боеприпасы — **магма-блоки**, обычные стрелы не нужны.

## Возможности
- Новый лук с кастомным названием и лором.
- Крафт и команда `/givefirebow`.
- Метеориты с частицами, уроном и поджогом (настраивается).
- Кулдаун между выстрелами.

## Требования
- **Paper 1.21.x** (совместимо с 1.21.11)
- **Java 21**

## Установка
1. Соберите `firebow-1.0.0.jar` (инструкция ниже).
2. Поместите jar в папку `plugins/` вашего Paper сервера.
3. Перезапустите сервер (или используйте `/reload`, но рекомендуется перезапуск).

## Сборка
### Требуется установленный Gradle и Java 21
Если команда `gradle` не распознана — установите Gradle и убедитесь, что он в `PATH`.

**Windows (PowerShell, пример через Chocolatey):**
```powershell
choco install temurin21 -y
choco install gradle -y
gradle --version
```
**Windows (альтернатива через Scoop):**
```powershell
scoop install temurin21
scoop install gradle
gradle --version
```
**Linux/macOS (SDKMAN):**
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
sdk install gradle
gradle --version
```

После установки Gradle:
**Linux/macOS:**
```bash
gradle build
```
**Windows (PowerShell/CMD):**
```bat
gradle build
```

Готовый jar будет в `build/libs/firebow-1.0.0.jar`.

## Команды и права
- `/givefirebow` — выдать Огненный лук.
  - Permission: `firebow.give` (по умолчанию `op`).

## Рецепт крафта
**Ингредиенты:** 3 Blaze Rod + 3 String

Схема (B = Blaze Rod, S = String, `.` = пусто):
```
B S .
S B .
B S .
```

## Настройка (config.yml)
```yml
damage: 8.0
fireSeconds: 4
explosionPower: 0.0
trailParticles: true
cooldownTicks: 12
magmaCost: 1
messageNoAmmo: "&cНужен магма-блок как боеприпас!"
```

- **damage** — урон метеорита.
- **fireSeconds** — сколько секунд горит цель.
- **explosionPower** — сила взрыва (0 = выключено).
- **trailParticles** — частицы следа.
- **cooldownTicks** — кулдаун в тиках (20 тиков = 1 сек).
- **magmaCost** — стоимость выстрела в магма-блоках.
- **messageNoAmmo** — сообщение при отсутствии боеприпасов.

## Как проверить в игре
1. Получите лук: `/givefirebow`.
2. Положите магма-блоки в инвентарь.
3. Стреляйте — должны появляться «метеориты» с частицами и уроном.

## Частые проблемы
- **`gradle`/`mvn` не распознаны** — установите Gradle (или Maven) и добавьте в `PATH`. См. раздел **Сборка**.
- **Плагин не загружается / ошибка версии Java** — убедитесь, что сервер запущен на Java 21.
- **Нет эффекта при выстреле** — проверьте, что это именно «Огненный лук» и есть магма-блоки.
- **Ошибки в консоли** — убедитесь, что используете Paper 1.21.x.
