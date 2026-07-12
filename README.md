# RPGMood

An ambient RPG plugin for Spigot/Paper servers. Scales hostile mobs by distance, biome, and nearby structures, gives players a personal adventure journal, and turns deaths into flavorful, biome-aware narrative messages.

## Features

- **Mob scaling** — hostile mobs gain levels (health, damage, armor, speed) based on distance from spawn, biome, and nearby structures; grants bonus XP on kill, broadcasts an announcement for dangerously high-level spawns, and shows level-tiered particle auras.
- **Adventure journal** — `/diary` opens a personal log of the player's journey.
- **Achievements** — `/rpgmood achievements` tracks 15 achievements across exploration, combat, farming, and survival.
- **Leaderboard** — `/rpgmood leaderboard` for top deaths, zone changes, or highest mob level killed.
- **Narrative death messages** — biome-, killer-, and PvP-aware flavor text instead of vanilla death messages.
- **Zones & ambience** — configurable zones with titles, sounds, particles, and flavor text, delivered via action bar or chat.
- **Harvest Moon-style farming** — `/rpgmood-farm` for seasonal crops with Bronze/Silver/Gold quality, discoverable cooking recipes with temporary Mood buffs, and animal husbandry (buy/feed/care for cows, chickens, sheep, goats).
- **Public API** — `PlayerZoneChangeEvent`, `MobScaleEvent`, `PlayerDeathMessageEvent`, `PlayerCropHarvestEvent` for other plugins to hook into.
- Soft-integrates with **PlaceholderAPI** and **WorldGuard**; exposes each mob's level via `PersistentDataContainer` for other plugins to read.

## Installation

1. Place `rpgmood.jar` in your server's `plugins/` folder
2. Restart the server

**Requires:** Paper/Spigot 1.20+ · Java 21+

## Documentation

→ **[Wiki](../../wiki)**

## Configuration

See `config.yml`, `triggers.yml`, `zones.yml`, and `farming.yml` in `src/main/resources` for mob scaling, death message, zone, and farming settings.
