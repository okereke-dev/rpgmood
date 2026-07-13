[![Paper](https://img.shields.io/badge/Paper-1.20.4%2B-blue?style=flat-square)](https://papermc.io) [![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square)](https://adoptium.net)

# 🌙 RPGMood

> Give your vanilla server a pulse — mobs that scale with danger, zones that react to where you are, and deaths that tell a story.

RPGMood layers atmosphere and a sense of place on top of unmodified vanilla survival. Hostile mobs grow stronger the further players wander from spawn, entering a new region shows a title and plays a sound, and dying gets a narrative death message instead of the flat vanilla one — all without touching loot tables, crafting, or PvP balance.

---

## ⚔️ Mob Scaling

Hostile mobs gain levels based on **distance from spawn**, **biome**, **nearby vanilla structures**, and **how many players are nearby** — shown live in the mob's name (`Lv. 12 Zombie`).

- Health, attack damage, armor, and movement speed all scale with level — health and damage share one even curve tuned to each mob's own real vanilla stats, so scaling feels consistent between mob types instead of a level-1 zombie and a level-1 Enderman ending up wildly mismatched
- Per-mob-type difficulty seeds (Zombie is easier than Wither Skeleton at the same distance)
- Biome and structure bonuses are fully configurable — make deserts scarier, or strongholds a real threat
- **Aggro range scales with the same curve** — weak early-game mobs notice you from a bit less far than vanilla, dangerous late-game mobs just as far or farther
- **Night and thunderstorm bonuses** stack extra difficulty on top of the existing distance/biome/structure/player scaling
- **Radial spawn protection** — hostile mobs no longer naturally spawn within a configurable radius of world spawn
- Structure-proximity checks are cached per region, so this stays cheap even with heavy mob spawning
- **Bonus XP on kill**, proportional to the mob's level — difficulty isn't just punishment
- **Server-wide announcement** when a dangerously high-level mob spawns (configurable threshold and cooldown)
- Every scaled mob's level is exposed via `PersistentDataContainer` for other plugins (e.g. loot plugins) to hook into
- Fully tunable in `config.yml`: growth curve, max level cap, night/thunder bonuses, spawn-protection radius, and every bonus source

---

## 👹 Mob Affixes

Scaled mobs can roll **1–2 affixes**, odds increasing with level, each flagged by a subtle particle marker and a one-time action-bar warning (name tags stay off by default):

- **Swift** — noticeably faster, up to 40% at higher levels
- **Wraith** — turns invisible; the particle marker is your only tell
- **Regenerating** — heals over time mid-fight
- **Bleeding** — on-hit chance to inflict a stacking bleed DOT on you
- **Poisonous** — on-hit chance to poison you
- **Chilling** — on-hit chance to slow you

Proc chances, durations, and bonus magnitudes are all tunable under `mob_scaling.affixes` in `config.yml`.

---

## 🗺️ Zones

Crossing into a new zone shows a title/subtitle, plays a sound and particles, and drops a short flavor line in chat a moment later.

- **Biome zones** — trigger when you enter a configured biome
- **WorldGuard zones** — trigger when you step inside a named WorldGuard region (optional integration)
- **Unclaimed areas** aren't left blank — every corner of the map gets a procedurally-combined name (e.g. *"Elden Meadows"*, *"the ash-choked Sunlit Barrows"*) built by recombining a pool of biome-appropriate adjectives and nouns, giving hundreds of unique names per biome without repeating
- **Zone titles are colored by local danger** — the same 5-tier palette RPGLoot uses for item rarity (gray → yellow → purple → green → gold), based on the mob level at that location
- **Optional sidebar scoreboard** (`/rpgmood toggle scoreboard`) shows your current zone, local danger level, and session kill count; a temporary danger-colored BossBar also flashes on every zone change
- **`/rpgmood zones`** lists every zone you've discovered — biome, danger level, first-seen date — lets you mark favorites, and shows distance to your nearest known zone
- **Per-zone music** — an optional namespaced sound key (resource-pack friendly) plays once on entry

---

## 💀 Death Messages

Every death gets a narrative message instead of the vanilla one — composed, not just picked from a list:

- A core line reacts to **cause of death**, **biome**, and **killer type** — including PvP kills, which correctly name the killing player
- Killer names get randomized synonyms (a Zombie might be called a "shambling snack" or "graveyard gremlin")
- An independent ~45% chance appends a closing flourish ("The night keeps its own score.")
- Location names get an independent descriptor prefix ("the storm-worn Elden Meadows")
- The same exact line won't fire twice in a row for the same player

Every death is also saved to the player's persistent adventure journal.

---

## 🖥️ GUI Menu

`/rpgmood` with no arguments (or `/rpgmood menu` explicitly) opens an inventory-based hub — every system below, in one place, no commands to memorize:

- **Adventure Journal**, **Achievements** (filterable by category), **Current Zone** (open to every player, not just admins), **Leaderboard**, **Farming**, **My Animals**, and **Settings** panels
- If **RPGLoot** is installed, an extra panel shows your equipped rarities, active set, and lifetime loot stats
- Admins get an **Admin Config** panel — steppers for spawn-protection radius, the mob-scaling curve, night/thunder bonuses, and the weather-effects toggle, writing straight to `config.yml` with no file editing required
- Every existing text command (`/diary`, `/rpgmood leaderboard`, `/rpgmood-farm`, etc.) still works exactly as before — the menu is an addition, not a replacement
- Tab-completion on `/rpgmood`, `/diary`, and `/rpgmood-farm`
- Custom join/quit messages (action-bar or chat, per player preference) with a distinct message and sound for a player's very first join

---

## 📖 Adventure Journal & Leaderboard

- `/diary` opens a written book listing the player's recent zone arrivals and deaths, timestamped and persisted across restarts
- `/rpgmood leaderboard` shows the top 10 players by deaths, zone changes, or highest-level mob killed

---

## 📈 Player Progression

`/rpgmood level` — a player progression level, separate from mob difficulty levels, shown right on the GUI menu's front page.

- Earned from scaled-mob kills and discovering new zones
- Every 5 levels raises your farm animal ownership cap by 1

---

## 🏆 Achievements

**31 achievements** across exploration, combat, survival, and farming — `/rpgmood achievements` shows your progress grouped by category, unlocked ones announced in chat and logged to your journal.

Not just grindy counters — the roster leans into things players actually chase for bragging rights: **Godslayer** (kill the Warden, Wither, Elder Guardian, *and* Ender Dragon), **Untouchable** (kill a level 20+ mob without ever taking damage from it), **Cartographer** (discover every zone you've configured), **Noah's Ark** (own one of every farm animal), and more. All persisted to `achievements.yml`, survive restarts.

If **RPGLoot** is installed, 6 additional Loot achievements unlock — **Legendary Hunter**, **Relic Bearer**, and the ultimate flex, **Godslayer's Arsenal** (collect all 4 of RPGLoot's boss-exclusive Artifacts). Zero setup required and zero effect if RPGLoot isn't installed.

---

## 🌾 Harvest Moon-Style Farming

A full farming, cooking, and animal-husbandry module (`/rpgmood-farm`), inspired by Harvest Moon NES/N64:

- **Crop quality** — every harvest scores Bronze/Silver/Gold based on soil fertility (per biome), nearby water, rain, and the zone's danger level
- **Four seasons** (Spring/Summer/Autumn/Winter, 30 MC days each) — each with its own growth multiplier and season-exclusive crops
- **Cooking & recipes** — discovered by experimenting with ingredients in a crafting table, grant temporary "Mood" buffs (bonus damage, regeneration, bonus XP)
- **Animal husbandry, no shop involved** — feed a wild cow/chicken/sheep/goat its normal vanilla breeding food and it's yours; pet, feed, milk, and shear claimed animals, and breeding automatically grows your herd under a shared owner. An affection system drives product quality, and neglect leads to sickness
- **Fine-grained permissions** — `rpgmood.player.farming` splits into `.season`/`.crops`/`.recipes`/`.animal` nodes if you want to restrict any single piece (all enabled by default)

---

## 🌦️ Ambient Time & Weather Events

RPGMood keeps the world feeling alive around you, not just when something notable happens:

- Scheduled messages and sounds tied to time-of-day (dawn, midday, dusk, midnight) and weather transitions (rain/thunder start and stop)
- **Ambient sounds** — low-probability birdsong and eerie ambience play near each player, independent of the scripted time/weather messages
- **Weather now has mechanical bite** — brief Darkness pulses during thunderstorms and small wind nudges during rain under open sky, both checked against the world's live weather state
- **Nether acid rain** — a periodic hazard for the one dimension vanilla weather doesn't reach
- Chance-based flavor messages on breaking configured blocks
- All fully defined in `triggers.yml` and `config.yml`

---

## 🔌 Integrations & Developer API

All optional — RPGMood works standalone and detects these automatically at startup.

- **PlaceholderAPI** — `%rpgmood_zone%`, `%rpgmood_area_danger%` for scoreboards and tab list
- **WorldGuard** — enables region-based zones
- **RPGLoot** — soft, zero-dependency integration in both directions: RPGLoot raises drop rarity floors near dangerous RPGMood mobs, and RPGMood's achievements react to RPGLoot's rarities/artifacts/sets. Neither plugin requires the other.
- **Public API for developers** — `PlayerZoneChangeEvent`, `MobScaleEvent`, `PlayerDeathMessageEvent`, `PlayerCropHarvestEvent` let other plugins hook into RPGMood without a hard dependency

---

## 🛠️ Admin Tools

- `/rpgmood info` shows your current zone and the mob difficulty level a Zombie, Skeleton, and Enderman would each get at your exact location — the fastest way to sanity-check your `mob_scaling` tuning without waiting for a real mob to spawn
- `/rpgmood reload` and `/rpgmood toggle` remain available for scripting and console use
- The in-game **Admin Config** GUI panel (see GUI Menu above) covers the same tuning with steppers, no file editing required
- `config.yml`, `zones.yml`, `triggers.yml`, and `farming.yml` all **auto-merge new keys** on startup or reload — updating RPGMood never silently wipes a config you've already customized

---

## 📦 Installation

```
1. Drop rpgmood.jar into your /plugins folder
2. Restart the server
3. Done — config.yml, zones.yml, triggers.yml, and farming.yml generate automatically
```

**Requirements:** Paper/Spigot 1.20.4+ · Java 21+ · No required dependencies

---

## 🔗 Links

- 📖 [Full Wiki](https://github.com/okereke-dev/rpgmood/wiki)
- 💻 [Source Code](https://github.com/okereke-dev/rpgmood)
- 🐛 [Report a Bug](https://github.com/okereke-dev/rpgmood/issues)
