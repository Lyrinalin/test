# ArmorStandEditor

ArmorStandEditor is a Paper 1.21.x plugin that lets you quickly select and edit armor stands with a clean GUI editor.

## Features
- Select armor stands via ray-trace (`/ase select` or just `/ase` while looking at a stand)
- Toggle arms, base plate, gravity, size, visibility, and marker
- Rotate armor stands by 15° increments
- Pose editor with adjustable axes and reset options
- Copy/paste poses between armor stands
- Equipment editor for hands and armor slots

## Commands
| Command | Description | Permission |
| --- | --- | --- |
| `/ase help` | Show help | `ase.use` |
| `/ase select` | Select armor stand in sight | `ase.select` |
| `/ase open` | Open editor GUI | `ase.edit` |
| `/ase copy` | Copy pose | `ase.copy` |
| `/ase paste` | Paste pose | `ase.paste` |
| `/ase reload` | Reload config | `ase.reload` |

## Permissions
- `ase.use` (default: op)
- `ase.select` (default: op)
- `ase.edit` (default: op)
- `ase.copy` (default: op)
- `ase.paste` (default: op)
- `ase.reload` (default: op)

## Configuration
`config.yml` includes:
- `selection-radius` (ray-trace distance)
- `angle-steps` (small/normal/large steps)
- `messages` and menu titles

## Build (Apache Maven 3.9.9)
This project ships with the Maven Wrapper. The wrapper jar is downloaded automatically on first use. Build with:

```bash
./mvnw clean package
```

The jar will be in `target/`.

## Installation
1. Build the jar with `./mvnw clean package`.
2. Copy `target/ArmorStandEditor-1.0.0.jar` into your Paper server `plugins/` folder.
3. Restart the server.
