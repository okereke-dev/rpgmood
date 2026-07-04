# Changelog

## [1.1.0] — 2026-07-04

### Added
- **Bonus XP on kill** — killing a scaled mob grants XP proportional to its level (`mob_scaling.bonus_xp_per_level`)
- **High-level mob announcements** — server-wide broadcast when a dangerously scaled mob spawns (configurable threshold/cooldown)
- **Cross-plugin level exposure** — every scaled mob's level is stored via `PersistentDataContainer` (`rpgmood:level`) for other plugins (e.g. loot plugins) to read
- **Leaderboard** — `/rpgmood leaderboard [deaths|zones|level]`, persisted in `stats.yml`
- **Zone particles** — the `particles` field in `zones.yml` (previously unused) now actually spawns particles on zone entry
- **PvP death messages** — a player killed by another player now gets a `killers.player` message naming the actual killer, instead of falling back to generic wilderness flavor text
- Per-biome location name descriptors (plains, dark_forest, desert, taiga, snowy_taiga, jungle, swamp, ocean, nether_wastes) in addition to the shared `default` pool

### Removed
- **Emotes** (`/emote`) — removed entirely rather than kept as a single generic, unfinished effect

## [1.0.0] — 2026-07-04

Initial release.

### Features
- **Mob scaling** — hostile mobs gain levels based on distance from spawn, biome, nearby structures, and nearby player count
- **Zones** — biome-based and WorldGuard region-based zones with custom titles, subtitles, sounds, particles, and flavor text; unclaimed areas get procedurally-assigned dynamic zone names
- **Adventure journal** (`/diary`) — a written book of the player's recent zone arrivals and deaths, persisted per-player
- **Narrative death messages** — biome-, cause-, and killer-aware flavor text with configurable location names
- **Ambient time/weather events** — scheduled messages and sounds on time-of-day and weather transitions (`triggers.yml`)
- **Block break triggers** — chance-based ambient messages on breaking configured blocks
- **Emotes** (`/emote`) — short particle/sound effect
- Soft-integrates with **PlaceholderAPI** (`%rpgmood_zone%`, `%rpgmood_area_danger%`) and **WorldGuard** (region-type zones)
- Admin command `/rpgmood reload|toggle|info` — `info` shows the current zone and mob difficulty at your location for tuning `config.yml`

### Configuration
- `config.yml` — mob scaling curve, messages, death message templates
- `zones.yml` — zone definitions (BIOME / WORLDGUARD)
- `triggers.yml` — time, weather, and block-break ambient events
