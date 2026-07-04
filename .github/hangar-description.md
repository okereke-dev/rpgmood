[![Paper](https://img.shields.io/badge/Paper-1.20.4%2B-blue?style=flat-square)](https://papermc.io) [![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square)](https://adoptium.net)

# 🌙 RPGMood

> Give your vanilla server a pulse — mobs that scale with danger, zones that react to where you are, and deaths that tell a story.

RPGMood layers atmosphere and a sense of place on top of unmodified vanilla survival. Hostile mobs grow stronger the further players wander from spawn, entering a new region shows a title and plays a sound, and dying gets a narrative death message instead of the flat vanilla one — all without touching loot tables, crafting, or PvP balance.

---

## ⚔️ Mob Scaling

Hostile mobs gain levels based on **distance from spawn**, **biome**, **nearby vanilla structures**, and **how many players are nearby** — shown live in the mob's name (`Lv. 12 Zombie`).

- Health, attack damage, armor, and movement speed all scale with level
- Per-mob-type difficulty seeds (Zombie is easier than Wither Skeleton at the same distance)
- Biome and structure bonuses are fully configurable — make deserts scarier, or strongholds a real threat
- Structure-proximity checks are cached per region, so this stays cheap even with heavy mob spawning
- Fully tunable in `config.yml`: health/damage/armor/speed growth per level, max level cap, and every bonus source

---

## 🗺️ Zones

Crossing into a new zone shows a title/subtitle, plays a sound, and drops a short flavor line in chat a moment later.

- **Biome zones** — trigger when you enter a configured biome
- **WorldGuard zones** — trigger when you step inside a named WorldGuard region (optional integration)
- **Unclaimed areas** aren't left blank — every corner of the map gets a procedurally-combined name (e.g. *"Elden Meadows"*, *"the ash-choked Sunlit Barrows"*) built by recombining a pool of biome-appropriate adjectives and nouns, giving hundreds of unique names per biome without repeating

---

## 💀 Death Messages

Every death gets a narrative message instead of the vanilla one — composed, not just picked from a list:

- A core line reacts to **cause of death**, **biome**, and **killer type**
- Killer names get randomized synonyms (a Zombie might be called a "shambling snack" or "graveyard gremlin")
- An independent ~45% chance appends a closing flourish ("The night keeps its own score.")
- Location names get an independent descriptor prefix ("the storm-worn Elden Meadows")
- The same exact line won't fire twice in a row for the same player

Every death is also saved to the player's persistent adventure journal.

---

## 📖 Adventure Journal & Emotes

- `/diary` opens a written book listing the player's recent zone arrivals and deaths, timestamped and persisted across restarts
- `/emote` plays a short particle/sound effect

---

## 🌦️ Ambient Time & Weather Events

Scheduled messages and sounds tied to time-of-day (dawn, midday, dusk, midnight) and weather transitions (rain/thunder start and stop), plus chance-based flavor messages on breaking configured blocks. All fully defined in `triggers.yml`.

---

## 🔌 Integrations

Both optional — RPGMood works standalone and detects these automatically at startup.

- **PlaceholderAPI** — `%rpgmood_zone%`, `%rpgmood_area_danger%` for scoreboards and tab list
- **WorldGuard** — enables region-based zones

---

## 🛠️ Admin Tools

`/rpgmood info` shows your current zone and the mob difficulty level a Zombie, Skeleton, and Enderman would each get at your exact location — the fastest way to sanity-check your `mob_scaling` tuning without waiting for a real mob to spawn.

---

## 📦 Installation

```
1. Drop rpgmood.jar into your /plugins folder
2. Restart the server
3. Done — config.yml, zones.yml, and triggers.yml generate automatically
```

**Requirements:** Paper/Spigot 1.20.4+ · Java 21+ · No required dependencies

---

## 🔗 Links

- 📖 [Full Wiki](https://github.com/okereke-dev/rpgmood/wiki)
- 💻 [Source Code](https://github.com/okereke-dev/rpgmood)
- 🐛 [Report a Bug](https://github.com/okereke-dev/rpgmood/issues)
