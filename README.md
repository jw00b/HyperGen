# HyperGen

HyperGen is a **high-performance world generation plugin** for Minecraft servers. It allows server admins and players to quickly generate large areas with advanced control over shapes, patterns, and performance.

---

## ⚡ Features

* High-speed world generation with three modes: `Normal`, `Pro`, and `Fast`.
* Multiple area selection shapes: `square` and `circle`.
* Generation patterns: `spiral` and `concentric`.
* Visual progress maps.
* Task queue management for multiple worlds.
* Detailed statistics tracking (chunks processed, speed, sessions).
* Notifications support (Discord & custom webhooks).
* Async and parallel chunk processing for maximum performance.
* Auto-save and memory management to prevent server crashes.

---

## 🛠 Installation

1. Download the latest HyperGen `.jar`.
2. Place it in your server's `plugins` folder.
3. Start the server to generate the configuration files.
4. Configure `config.yml` according to your preferences.
5. Restart or reload the server.

---

## ⚙ Configuration

Key configuration options (`config.yml`):

```yaml
language: en
update-interval: 5
auto-save-interval: 300
max-chunks-per-tick: 8

normal-mode:
  chunks-per-second: 40

pro-mode:
  target-tps: 19.5
  max-chunks-per-tick: 16

fast-mode:
  chunks-per-tick: 32
  disable-spawning: true
  kick-players: true
  kick-message: '&cServer is processing chunks'

performance:
  async-chunk-loading: true
  parallel-processing: true
  max-concurrent-chunks: 100
```

> Tip: `Fast` mode is powerful but can kick players and disable spawning/events. Always confirm before using.

---

## 🕹 Commands

HyperGen provides a full command system. Most commands require `hypergen.use` permission; advanced commands require specific permissions.

| Command                                 | Permission         | Description                                                                    |
| --------------------------------------- | ------------------ | ------------------------------------------------------------------------------ |
| `/hypergen start [mode]`                | `hypergen.use`     | Start world generation for your selected area. Modes: `normal`, `pro`, `fast`. |
| `/hypergen pause`                       | `hypergen.use`     | Pause the current generation task.                                             |
| `/hypergen continue`                    | `hypergen.use`     | Continue a paused generation task.                                             |
| `/hypergen cancel`                      | `hypergen.use`     | Cancel the current generation task.                                            |
| `/hypergen world [worldName]`           | `hypergen.use`     | Select a world for generation. Defaults to your current world.                 |
| `/hypergen shape <shape>`               | `hypergen.use`     | Set the shape of the generation area (`square`, `circle`).                     |
| `/hypergen center [x] [z]`              | `hypergen.use`     | Set the center of the generation area. Defaults to your location.              |
| `/hypergen radius <number>`             | `hypergen.use`     | Set the radius for circular selections.                                        |
| `/hypergen worldborder`                 | `hypergen.use`     | Select the world border as your area.                                          |
| `/hypergen spawn`                       | `hypergen.use`     | Select the world spawn point as your area.                                     |
| `/hypergen corners <x1> <z1> <x2> <z2>` | `hypergen.use`     | Define rectangular area corners.                                               |
| `/hypergen pattern <pattern>`           | `hypergen.use`     | Set generation pattern (`spiral`, `concentric`).                               |
| `/hypergen selection`                   | `hypergen.use`     | Show current selection info.                                                   |
| `/hypergen silent`                      | `hypergen.use`     | Toggle silent mode.                                                            |
| `/hypergen quiet <interval>`            | `hypergen.use`     | Set interval for quiet progress messages.                                      |
| `/hypergen progress`                    | `hypergen.use`     | Show detailed task progress.                                                   |
| `/hypergen map`                         | `hypergen.map`     | Open a visual map of the task progress.                                        |
| `/hypergen stats [world]`               | `hypergen.stats`   | Show generation statistics.                                                    |
| `/hypergen queue <action>`              | `hypergen.queue`   | Manage task queue (`add`, `remove`, `list`, `clear`).                          |
| `/hypergen info`                        | `hypergen.info`    | Show plugin information.                                                       |
| `/hypergen version`                     | `hypergen.version` | Show plugin version.                                                           |
| `/hypergen list`                        | `hypergen.list`    | List all active tasks.                                                         |
| `/hypergen reload`                      | `hypergen.use`     | Reload configuration and messages.                                             |
| `/hypergen trim`                        | `hypergen.use`     | Trim world chunks.                                                             |
| `/hypergen speed`                       | `hypergen.use`     | Show generation speed (chunks/s).                                              |
| `/hypergen eta`                         | `hypergen.use`     | Show estimated time remaining.                                                 |
| `/hypergen help`                        | `hypergen.use`     | Display help message.                                                          |

> Tip: `/hypergen confirm` is required for `fast` mode due to server impact.

---

## 📊 Generation Modes

| Mode     | Description                                                                     |
| -------- | ------------------------------------------------------------------------------- |
| `Normal` | Default, stable generation with moderate speed.                                 |
| `Pro`    | Optimized for large areas; adjusts chunk processing based on TPS.               |
| `Fast`   | Maximum speed, disables spawning/events and may kick players. Use with caution. |

---

## 📈 Statistics & Progress

* Tracks processed chunks, current chunk, and total chunks.
* Calculates speed in chunks/sec.
* Shows estimated time remaining (ETA) for tasks.
* Supports auto-saving and session tracking.

---

## 📡 Notifications

* Discord webhook support.
* Custom webhooks.
* Notify on start, progress, completion, and cancellation.
* Configurable notification interval.

---

## ⚖️ Permissions

* `hypergen.use` – Use general commands.
* `hypergen.map` – Access progress maps.
* `hypergen.stats` – View statistics.
* `hypergen.queue` – Manage generation queue.
* `hypergen.info` – View plugin info.
* `hypergen.version` – Check version.
* `hypergen.list` – List active tasks.

---

## 📝 License

Distributed under MIT License.
