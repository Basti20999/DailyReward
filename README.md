# DailyReward

A lightweight Paper/Spigot 1.21 plugin that gives players a configurable daily reward through a GUI — with streaks, a rotating weekly reward pool, a live countdown timer, admin tools, and optional PlaceholderAPI support.

## Features

- **Clickable GUI inventory** with a live ticking countdown
- **Streak system** — claim consecutively for bonus rewards (configurable tiers)
- **Weekly rotating rewards** — a different reward for each day of a 7-day cycle, visualized in the GUI
- **Admin commands** — `/daily reset`, `/daily give`, `/daily stats`, `/daily top`, `/daily reload`
- **Persistent storage** via SQLite (default, zero setup) or MySQL — with automatic migration from v1.x databases
- **Async database writes** — no lag spikes under load
- **PlaceholderAPI integration** (optional, soft-depend)
- **Hex color codes** (`#RRGGBB`) and legacy codes (`&a`, `&b`, ...)
- **Safe fallbacks** for invalid materials, sounds, slots — with warnings in the console

## Requirements

- Java 21+
- Paper / Spigot 1.21+
- (Optional) PlaceholderAPI

## Installation

1. Build the JAR (`mvn clean package`) or grab a release.
2. Place `DailyReward-2.0.jar` in your server's `plugins/` folder.
3. Start the server — a default `config.yml` is generated.
4. Edit `plugins/DailyReward/config.yml` to match your economy/keys/gems plugins.
5. Run `/daily reload` or restart the server.

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/daily` | — | Open the daily reward GUI (alias: `/reward`, `/dr`) |
| `/daily stats [player]` | — (self) / `dailyreward.admin.stats` (others) | Show streak, total claims, last/next claim |
| `/daily top` | — | Show top 10 streaks |
| `/daily help` | — | List available commands |
| `/daily reset <player>` | `dailyreward.admin.reset` | Reset a player's cooldown and streak |
| `/daily give <player>` | `dailyreward.admin.give` | Force-give a reward (bypasses cooldown, increments streak) |
| `/daily reload` | `dailyreward.admin.reload` | Reload the config without restarting |

All admin perms default to `op`. Grant `dailyreward.admin` to get all of them.

## PlaceholderAPI

With PlaceholderAPI installed, the following placeholders are available:

| Placeholder | Description |
|-------------|-------------|
| `%dailyreward_streak%` | Current streak count |
| `%dailyreward_total%` | Total lifetime claims |
| `%dailyreward_ready%` | `true` if the player can claim now |
| `%dailyreward_cooldown%` | Formatted remaining time (e.g. `12h 34m 05s`) or `Ready!` |
| `%dailyreward_next_day%` | Upcoming day in the 7-day weekly rotation (1–7) |
| `%dailyreward_last_claim%` | Millis since epoch of the last claim |

## Configuration highlights

### Streak bonuses

```yaml
streak:
  enabled: true
  grace_multiplier: 2.0   # players can miss up to (cooldown * 2) and keep the streak
  bonus_at:
    7:  { money_multiplier: 1.5, extra_keys: 1, bonus_gems: 100 }
    14: { money_multiplier: 2.0, extra_keys: 2, bonus_gems: 250 }
    30: { money_multiplier: 3.0, extra_keys: 5, bonus_gems: 1000 }
```

### Weekly rotation

```yaml
weekly_rewards:
  enabled: true
  rotation:
    1: { money: [100, 300],  keys: { type: common, amount: 1 },    gems: 100 }
    # ...
    7: { money: [2500, 5000], keys: { type: legendary, amount: 1 }, gems: 1500 }
```

Disable weekly rewards (`enabled: false`) to use the flat `money`/`keys`/`gems` blocks on every claim.

### Console commands dispatched on claim

Use placeholders `{player}`, `{amount}`, `{type}`. Leave blank to skip.

```yaml
commands:
  money_give: "eco give {player} {amount}"
  keys_give: "crates key give {player} {type} {amount}"
  gems_give: "gems give {player} {amount}"
```

### Database

SQLite (default, no setup):
```yaml
database:
  type: sqlite
```
Database file: `plugins/DailyReward/rewards.db`. v1.x databases are auto-migrated (new columns added on first start of v2).

MySQL:
```yaml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: dailyreward
    username: root
    password: "yourpassword"
    use_ssl: false
```

## Building

```bash
mvn clean package
```

The shaded JAR is written to `target/DailyReward-2.0.jar`.

## Notes

- Invalid materials, sounds, slots, or ranges in the config produce a console warning and fall back to safe defaults instead of crashing.
- Reward claims are concurrency-safe: rapid-clicking the reward item cannot produce duplicate claims.
- Database writes happen off the main thread; players see instant state updates while the DB catches up in the background.

## License

This project is provided as-is without a specific license. Feel free to modify it for your own server.
