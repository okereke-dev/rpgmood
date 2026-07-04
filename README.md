# RPGMood

An ambient RPG plugin for Spigot/Paper servers. Scales hostile mobs by distance, biome, and nearby structures, gives players a personal adventure journal, and turns deaths into flavorful, biome-aware narrative messages.

## Features

- **Mob scaling** — hostile mobs gain levels (health, damage, armor, speed) based on distance from spawn, biome, and nearby structures; grants bonus XP on kill and broadcasts an announcement for dangerously high-level spawns.
- **Adventure journal** — `/diary` opens a personal log of the player's journey.
- **Leaderboard** — `/rpgmood leaderboard` for top deaths, zone changes, or highest mob level killed.
- **Narrative death messages** — biome-, killer-, and PvP-aware flavor text instead of vanilla death messages.
- **Zones & ambience** — configurable zones with titles, sounds, particles, and flavor text.
- Soft-integrates with **PlaceholderAPI** and **WorldGuard**; exposes each mob's level via `PersistentDataContainer` for other plugins to read.

## Installation

1. Place `rpgmood.jar` in your server's `plugins/` folder
2. Restart the server

**Requires:** Paper/Spigot 1.20+ · Java 21+

## Documentation

→ **[Wiki](../../wiki)**

## Configuration

See `config.yml`, `triggers.yml`, and `zones.yml` in `src/main/resources` for mob scaling, death message, and zone settings.
