# Changelog

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
