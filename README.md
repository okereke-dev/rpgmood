# RPGMood

An ambient RPG plugin for Spigot/Paper servers. Scales hostile mobs by distance, biome, and nearby structures, gives players a personal adventure journal, and turns deaths into flavorful, biome-aware narrative messages.

## Features

- **Mob scaling** — hostile mobs gain levels (health, damage, armor, speed) based on distance from spawn, biome, and nearby structures.
- **Adventure journal** — `/diary` opens a personal log of the player's journey.
- **Narrative death messages** — biome- and killer-aware flavor text instead of vanilla death messages.
- **Zones & ambience** — configurable zones with ambient effects and ambient sound/particle tasks.
- **Emotes** — player emote command.
- Soft-integrates with **PlaceholderAPI** and **WorldGuard**.

## Installation

1. Place `rpgmood.jar` in your server's `plugins/` folder
2. Restart the server

**Requires:** Paper/Spigot 1.20+ · Java 21+

## Documentation

→ **[Wiki](../../wiki)**

## Configuration

See `config.yml`, `triggers.yml`, and `zones.yml` in `src/main/resources` for mob scaling, death message, and zone settings.
