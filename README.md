# MeteorBow (Paper 1.21.x)

Плагин добавляет кастомный лук «Огненный лук», который стреляет метеоритами и использует магма-блоки как боеприпасы.

## Требования
- JDK 21
- Gradle 8.x (установлен в системе)
- Paper 1.21.x

## Сборка
### Windows
```bat
scripts\build_and_copy.bat
```

### Linux/macOS
```bash
chmod +x scripts/build_and_copy.sh
scripts/build_and_copy.sh
```

Скрипты используют установленный в системе Gradle (`gradle`).
После сборки jar будет в `build/libs/MeteorBow-<version>.jar`.

## Установка
1. Скопируйте `MeteorBow-<version>.jar` в папку `plugins` вашего сервера Paper.
2. Перезапустите сервер.

## Команды
- `/meteorbow give` — выдать «Огненный лук» (permission: `meteorbow.admin`).
- `/meteorbow reload` — перезагрузить конфиг (permission: `meteorbow.admin`).

## Как получить лук без команд
Скрафтите по рецепту:
```
BSB
S S
 B 
```
`B = blaze_rod`, `S = string`.

## Как использовать
1. Держите «Огненный лук» в руке.
2. Иметь в инвентаре минимум 1 `magma_block`.
3. Стреляйте как обычным луком.
