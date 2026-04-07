# DailyReward

A lightweight Spigot/Paper Minecraft plugin that gives players a configurable daily reward through a GUI.

## Features

- Clickable GUI inventory for claiming rewards
- Configurable cooldown (default: 24 hours)
- Supports hex color codes (`#RRGGBB`) and legacy color codes (`&a`, `&b`, ...)
- Fully configurable rewards: money, keys, gems (via console commands)
- Configurable sounds, GUI layout, materials, and messages
- Safe fallbacks for invalid materials and sounds (logs a warning instead of crashing)

## Requirements

- Java 21+
- Paper / Spigot 1.21+

## Installation

1. Download or build the JAR (see [Building](#building)).
2. Place the JAR in your server's `plugins/` folder.
3. Start or reload the server — a default `config.yml` will be generated.
4. Edit `plugins/DailyReward/config.yml` to match your server's plugins and economy setup.
5. Reload the config with `/reload` or restart the server.

## Commands

| Command | Alias | Description |
|---------|-------|-------------|
| `/daily` | `/reward` | Opens the daily reward GUI |

## Configuration

The default `config.yml` is generated on first run. Key sections:

### Cooldown

```yaml
cooldown_hours: 24
```

### Rewards

```yaml
money:
  min: 100
  max: 1000

keys:
  type: rare
  amount: 1

gems: 250
```

### Commands

Console commands dispatched on reward claim. Adjust these to match your server's economy, keys, and gems plugins. Use the placeholders `{player}`, `{amount}`, and `{type}`.

```yaml
commands:
  money_give: "eco give {player} {amount}"
  keys_give: "crates key give {player} {type} {amount}"
  gems_give: "gems give {player} {amount}"
```

Set a command to `""` (empty string) to skip it entirely.

### GUI

```yaml
gui:
  title: "&8Daily Reward"
  size: 27
  reward_slot: 13
  reward_material: CHEST
  reward_name_ready: "#00FF00&lCLAIM REWARD!"
  reward_name_cooldown: "#FF0000&lALREADY CLAIMED"
  reward_lore:
    - "&f- #00FF00$100 – $1,000 &7(random)"
    - "&f- 1x #00FF00&lRARE KEY"
    - "&f- #FF55FF250 Gems"
  border_material: BLACK_STAINED_GLASS_PANE
  border_slots: [0,1,2,3,4,5,6,7,8,18,19,20,21,22,23,24,25,26]
```

### Sounds

Use any valid Bukkit `Sound` enum name (e.g. `ENTITY_PLAYER_LEVELUP`).

```yaml
sounds:
  open:
    sound: BLOCK_NOTE_BLOCK_PLING
    volume: 1.0
    pitch: 1.5
  success:
    sound: ENTITY_PLAYER_LEVELUP
    volume: 1.0
    pitch: 1.0
  error:
    sound: ENTITY_VILLAGER_NO
    volume: 1.0
    pitch: 1.0
```

## Building

```bash
mvn clean package
```

The shaded JAR will be output to `target/DailyReward-1.0.jar`.

## Notes

- Reward times are stored **in memory only** and reset on server restart. Players can claim again after a restart.
- All invalid material or sound names in the config will produce a warning in the console and fall back to safe defaults.

## License

This project is provided as-is without a specific license. Feel free to modify it for your own server.
