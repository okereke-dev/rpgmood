# Changelog

## [1.3.0] — 2026-07-12

### Added
- **16 new achievements** (15 → 31 total), grouped into a new `Category` (Exploration/Combat/Survival/Farming/Loot) shown in `/rpgmood achievements`:
  - Exploration: Cartographer (discover every configured zone)
  - Combat: Untouchable (damage-free kill), Godslayer (kill all 4 major bosses), Overkill (level 30+ kill that drops a Legendary RPGLoot item)
  - Survival: Storm Chaser (survive a lightning strike), Death Tourist (die in 8 biomes)
  - Farming: Four Seasons, Noah's Ark, Beloved, Green Thumb
  - Loot: Legendary Hunter, Relic Bearer, Godslayer's Arsenal, Matching Set, Head to Toe, Dressed to Kill
- **RPGLoot integration** — six new Loot achievements read RPGLoot's item/player `PersistentDataContainer` tags directly (no Maven dependency either way, same soft-integration convention RPGLoot already uses to read `rpgmood:level`). New `RPGLootIntegration` and `RPGLootAchievementListener`.
- `SeasonManager`'s season-change broadcast now actually unlocks `seasons_first`/`four_seasons` — it never did before (a gap from 1.2.0: the achievement was defined but never wired to the season-change event).

### Fixed
- `AchievementManager.ALL_ACHIEVEMENTS` was a `Set.of(...)`, so `/rpgmood achievements` rendered in unspecified (JVM-dependent) order. Switched to an ordered `List`.

## [1.2.0] — 2026-07-12

### Added
- **Public API for developers** — `PlayerZoneChangeEvent`, `MobScaleEvent`, `PlayerDeathMessageEvent`, `PlayerCropHarvestEvent` (`com.ricardo.rpgmood.api`), so other plugins (e.g. loot plugins) can hook into RPGMood without a hard dependency
- **bStats metrics** — registered with a real plugin ID
- **Mob level particle auras** (`MobAuraEffect`) — subtle, level-tiered coloured particles around scaled mobs (blue/yellow/orange/red as level increases), only rendered for mobs near a player
- **Achievement system** — 15 achievements across exploration, combat, farming, and survival (`AchievementManager`), persisted to `achievements.yml`; `/rpgmood achievements` shows unlocked/locked progress
- **Action-bar ambient messages** (`MessageService`) — ambient/zone/achievement messages now default to the action bar instead of chat, with a per-player toggle
- **Harvest Moon-style farming module** (`com.ricardo.rpgmood.farming`):
  - Crop quality (Bronze/Silver/Gold) based on soil fertility, nearby water, weather, and zone danger
  - Four-season cycle (Spring/Summer/Autumn/Winter, 30 MC days each) affecting growth rate and available crops
  - Cooking & recipe discovery — recipes are learned by experimenting in a crafting table and grant temporary "Mood" buffs (Fortified, Comforted, Inspired, Agotado)
  - Animal husbandry — buy, name, feed, and collect products from cows/chickens/sheep/goats (`/rpgmood-farm animal buy|list|info`)
  - `/rpgmood-farm season|crops|recipes [all]|animal` command
  - Recipe discovery now persists to `recipes.yml` across restarts

### Fixed
- **`MessageService.toggle()` rewriting the whole config** — now delegates to `ConfigManager.savePlayerToggle()` like the rest of the player toggles, instead of calling `saveConfig()` directly

## [1.1.1] — 2026-07-11

### Fixed
- **Biome bonuses for variant biomes** (`MobScalingService.getBiomeBonus()`) — biomes like MEADOW, SUNFLOWER_PLAINS, BAMBOO_JUNGLE now correctly inherit bonuses from their parent biome group (PLAINS, FOREST, JUNGLE, etc.) instead of always returning 0
- **Structure scan lag** (`MobScalingService.scanStructureBonus()`) — increased grid cache granularity from 128→512 blocks and added chunk-generation pre-checks to avoid expensive `locateNearestStructure()` calls in ungenerated areas; structure cache is now invalidated on `/rpgmood reload`
- **Zone fuzzy matching** (`ZoneManager.isBiomeMatch()`) — tightened pattern matching with underscore-delimited tokens to prevent false-positive matches (e.g. "FOR" no longer matches "FOREST")
- **Diary page overflow** (`DiarioCommand`) — replaced fixed-entry-per-page layout with dynamic character-aware page splitting to respect Minecraft's 256-char book page limit
- **Excessive disk I/O** (`PlayerStatsService`) — stat saves are now debounced (5s window), coalescing multiple rapid changes into a single write
- **Memory leaks** (`AmbientTask`) — added periodic eviction (every 5 min) of stale entries from `lastTriggeredEvents` and `lastWeatherType` maps
- **Biome alias mismatch** (`DeathMessageListener`) — aligned `BIOME_ALIAS` with `ZoneManager.BIOME_GROUP` so forest, mushroom, and end biomes map consistently across both systems
- **Non-contextual death messages** (`DeathMessageListener.selectMessage()`) — replaced random-category mixing with priority-based selection: killer > cause > biome > armed > fallback; each message is now thematically relevant
- **Config toggle efficiency** (`RPGMoodCommand`) — `/rpgmood toggle` now persists only the player's toggle value instead of rewriting the entire `config.yml`

### Added
- `MobScalingService.invalidateStructureCache()` — public method for cache invalidation
- `ConfigManager.savePlayerToggle()` — targeted persistence for player preference toggles
- New biome sections in `config.yml` (`forest`, `mushroom`, `end`) with location names, descriptors, and flavour text for complete biome coverage

## [1.1.0] — 2026-07-04

### Added
- **Bonus XP on kill** — killing a scaled mob grants XP proportional to its level (`mob_scaling.bonus_xp_per_level`)
- **High-level mob announcements** — server-wide broadcast when a dangerously scaled mob spawns (configurable threshold/cooldown)
- **Cross-plugin level exposure** — every scaled mob's level is stored via `PersistentDataContainer` (`rpgmood:level`) for other plugins (e.g. loot plugins) to read
- **Leaderboard** — `/rpgmood leaderboard [deaths|zones|level]`, persisted in `stats.yml`
- **Zone particles** — the `particles` field in `zones.yml` (previously unused) now actually spawns particles on zone entry
- **PvP death messages** — a player killed by another player now gets a `killers.player` message naming the actual killer, instead of falling back to generic wilderness flavor text
- Per-biome location name descriptors (plains, dark_forest, desert, taiga, snowy_taiga, jungle, swamp, ocean, nether_wastes) in addition to the shared `default` pool
- **Zone title hysteresis** — a zone change now needs a short dwell time (1.5s on foot, 3s while gliding/riding a vehicle) before feedback fires, eliminating title spam from border pacing and fast pass-throughs on horseback, boat, minecart, or elytra
- **Tiered zone feedback** — the big Title/Subtitle now has its own 5-minute "seen recently" memory per zone (separate from the ambient sound/chat cooldown) and a shorter duration while moving fast, so it doesn't linger over your view
- `/rpgmood toggle titles` — opt out of just the Title/Subtitle overlay while keeping sound and chat flavor
- Fixed: `/rpgmood toggle` (master) is now actually respected by zone feedback — previously it only affected time/weather ambient messages

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
