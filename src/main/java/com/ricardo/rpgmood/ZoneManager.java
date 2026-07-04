package com.ricardo.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ZoneManager {

    private static final int ZONE_SIZE = 256;
    private static final String DYNAMIC_ZONE_PREFIX = "DYNAMIC_ZONE";
    private static final String DEFAULT_SOUND = "ambient.weather.wind_light";
    private static final int MAX_ASSIGNED_ZONE_NAMES = 2000;

    private final RPGMoodPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, String> lastZones = new HashMap<>();
    private final Map<String, String> assignedZoneNames = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_ASSIGNED_ZONE_NAMES;
        }
    };

    private static final Map<String, String> BIOME_GROUP = Map.ofEntries(
            Map.entry("SUNFLOWER_PLAINS", "PLAINS"),
            Map.entry("MEADOW", "PLAINS"),
            Map.entry("FLOWER_FOREST", "FOREST"),
            Map.entry("FOREST", "FOREST"),
            Map.entry("BIRCH_FOREST", "FOREST"),
            Map.entry("OLD_GROWTH_BIRCH_FOREST", "FOREST"),
            Map.entry("OLD_GROWTH_PINE_TAIGA", "SNOWY_TAIGA"),
            Map.entry("OLD_GROWTH_SPRUCE_TAIGA", "SNOWY_TAIGA"),
            Map.entry("GIANT_TREE_TAIGA", "TAIGA"),
            Map.entry("GIANT_SPRUCE_TAIGA", "TAIGA"),
            Map.entry("ICE_SPIKES", "SNOWY_TAIGA"),
            Map.entry("SNOWY_PLAINS", "SNOWY_TAIGA"),
            Map.entry("SNOWY_MOUNTAINS", "MOUNTAINS"),
            Map.entry("FROZEN_PEAKS", "MOUNTAINS"),
            Map.entry("JAGGED_PEAKS", "MOUNTAINS"),
            Map.entry("GROVE", "DARK_FOREST"),
            Map.entry("BAMBOO_JUNGLE", "JUNGLE"),
            Map.entry("MANGROVE_SWAMP", "SWAMP"),
            Map.entry("CRIMSON_FOREST", "NETHER_CRIMSON"),
            Map.entry("WARPED_FOREST", "NETHER_WARPED"),
            Map.entry("BASALT_DELTAS", "NETHER_BASALT"),
            Map.entry("SOUL_SAND_VALLEY", "NETHER_SOUL"),
            Map.entry("NETHER_WASTES", "NETHER_WASTES"),
            Map.entry("OCEAN", "OCEAN"),
            Map.entry("LUKEWARM_OCEAN", "OCEAN"),
            Map.entry("WARM_OCEAN", "OCEAN"),
            Map.entry("DEEP_OCEAN", "OCEAN"),
            Map.entry("COLD_OCEAN", "OCEAN"),
            Map.entry("FROZEN_OCEAN", "OCEAN"),
            Map.entry("RIVER", "RIVER"),
            Map.entry("FROZEN_RIVER", "RIVER"),
            Map.entry("BEACH", "BEACH"),
            Map.entry("STONE_SHORE", "BEACH"),
            Map.entry("WARM_BEACH", "BEACH"),
            Map.entry("SNOWY_BEACH", "BEACH"),
            Map.entry("MUSHROOM_FIELDS", "MUSHROOM"),
            Map.entry("MUSHROOM_FIELD_SHORE", "MUSHROOM"),
            Map.entry("THE_END", "END"),
            Map.entry("END_MIDLANDS", "END"),
            Map.entry("END_HIGHLANDS", "END"),
            Map.entry("END_BARRENS", "END"),
            Map.entry("SMALL_END_ISLANDS", "END")
    );

    private static final Map<String, List<String>> BIOME_NAME_POOLS = Map.ofEntries(
            Map.entry("PLAINS", List.of(
                    "Elden Meadows",
                    "Sunlit Barrows",
                    "Whispering Grasslands",
                    "Amber Flats",
                    "Golden Vale",
                    "Hearthwild Fields",
                    "Silverwind Pastures",
                    "Starlit Reaches",
                    "Dawnflow Prairie",
                    "Mossbright Downs",
                    "Songwind Expanse",
                    "Honeyed Plains",
                    "Verdant Strands",
                    "Amberlee Haven",
                    "Sagebrush Hollow",
                    "Opal Steppe",
                    "Meadowglen Rise",
                    "Thundermoor Flats",
                    "Briarwind Glade",
                    "Goldenmist Fields"
            )),
            Map.entry("FOREST", List.of(
                    "Shadowwood Grove",
                    "Emberleaf Thicket",
                    "Sylvan Hollow",
                    "Oakenshade Vale",
                    "Fernbloom Copse",
                    "Mossmantle Glade",
                    "Cinderbark Wood",
                    "Greenfire Hollow",
                    "Moonfen Grove",
                    "Willowshade Dell",
                    "Berrywind Wood",
                    "Ancient Leafwood",
                    "Glimmerbark Vale",
                    "Mistwood Hollow",
                    "Frostbranch Grove",
                    "Wildroot Stand",
                    "Sunmoss Thicket",
                    "Ironleaf Glade",
                    "Wispwood Fen",
                    "Hollowgrove Reach"
            )),
            Map.entry("DARK_FOREST", List.of(
                    "Gloomfen Vale",
                    "Nightbriar Woods",
                    "Umbral Grove",
                    "Witchroot Hollow",
                    "Blackthorn Thicket",
                    "Ravenwood Fen",
                    "Mournshade Coppice",
                    "Duskgrove Hollow",
                    "Harrowmist Wood",
                    "Shadowfen Glade",
                    "Bleakroot Dell",
                    "Nocturne Stand",
                    "Grimleaf Thicket",
                    "Ashenbriar Wood",
                    "Dreadhollow Vale",
                    "Sablegrove Reach",
                    "Cindershade Forest",
                    "Fenn of Whispers",
                    "Obsidian Glade",
                    "Chillbark Hollow"
            )),
            Map.entry("TAIGA", List.of(
                    "Frostpine Reach",
                    "Boreal Spine",
                    "Icewind Thicket",
                    "Pinejaw Grove",
                    "Snowdrift Hollow",
                    "Glacierfen Stand",
                    "Winteroak Vale",
                    "Hailbark Woods",
                    "Crystalpine Glade",
                    "Stormroot Camp",
                    "Blizzard Hollow",
                    "Silverfir Thicket",
                    "Aurorawind Grove",
                    "Mistpine Reach",
                    "Frostfell Hollow",
                    "Icetooth Stand",
                    "Coldspire Woods",
                    "Snowleaf Vale",
                    "Windchill Glade",
                    "Shiverpine Thicket"
            )),
            Map.entry("SNOWY_TAIGA", List.of(
                    "Silverpine Tundra",
                    "Frostfell Basin",
                    "Snowmantle Ravine",
                    "Glacierveil Hollow",
                    "Pearlwind Flats",
                    "Shiverfall Glen",
                    "Polarbranch Reach",
                    "Icevein Woods",
                    "Snowdrift Plateau",
                    "Starfrost Hollow",
                    "Winterlace Vale",
                    "Crystalvein Rise",
                    "Frostshine Morass",
                    "Blizzardmere Glade",
                    "Glinting Fen",
                    "Thundersnow Reach",
                    "Bitterroot Hollow",
                    "Aurorafield Basin",
                    "Hailstone Garden",
                    "Moonfrost Thicket"
            )),
            Map.entry("JUNGLE", List.of(
                    "Lianasong Canopy",
                    "Emerald Wilds",
                    "Vinedray Hollow",
                    "Rainflare Thicket",
                    "Orchidshade Groves",
                    "Silkroot Jungle",
                    "Templebloom Glade",
                    "Sunshard Canopy",
                    "Mistvine Dell",
                    "Jadeleaf Wilds",
                    "Creepervine Hollow",
                    "Thunderleaf Thicket",
                    "Blossomreach Jungle",
                    "Tanglebark Grove",
                    "Moonvine Vale",
                    "Ancientroot Canopy",
                    "Sundew Hollow",
                    "Lushmire Wilds",
                    "Emeraldmoss Glade",
                    "Vinewhisper Jungle"
            )),
            Map.entry("SWAMP", List.of(
                    "Bogfen Mire",
                    "Murkwater Fen",
                    "Sorrowfen",
                    "Mireglow Hollow",
                    "Thornmoss Swale",
                    "Rotwood Bog",
                    "Glintwisp Marsh",
                    "Shadowmud Flats",
                    "Frogmouth Fen",
                    "Palewater Hollow",
                    "Dreadreed Swamp",
                    "Mudwhisper Glenn",
                    "Slickroot Mire",
                    "Moonmire Hollow",
                    "Fenblood Glade",
                    "Sootfen Reach",
                    "Brimstone Marsh",
                    "Blackwater Dell",
                    "Gloomroot Bog",
                    "Reedshroud Fen"
            )),
            Map.entry("DESERT", List.of(
                    "Mirage Dunes",
                    "Sandrift Expanse",
                    "Amberwaste",
                    "Sunshard Flats",
                    "Oasisfall Sands",
                    "Dustveil Plateau",
                    "Scorchwind Barrens",
                    "Duneblack Reach",
                    "Cindercrest Dunes",
                    "Saltflare Basin",
                    "Sunstroke Fields",
                    "Glassbluff Shallows",
                    "Copperheat Flats",
                    "Stormdrift Wastes",
                    "Burning Spire Sands",
                    "Sunbone Barrens",
                    "Gilded Dune Vale",
                    "Duskfire Expanse",
                    "Crimson Sand Reach",
                    "Goldenwind Flats"
            )),
            Map.entry("BADLANDS", List.of(
                    "Redstone Barrens",
                    "Titanrock Spires",
                    "Copper Plateau",
                    "Cragburn Flats",
                    "Scarred Mesa",
                    "Ochre Rift",
                    "Clayfire Badlands",
                    "Sundermarch Flats",
                    "Alderpeak Range",
                    "Spinehill Wastes",
                    "Briarcrag Expanse",
                    "Dustfall Heights",
                    "Ironcliff Mesa",
                    "Stormshard Plateau",
                    "Pyrestone Wastes",
                    "Fossilwind Flats",
                    "Rustspire Barrens",
                    "Sandsunder Range",
                    "Canyonflare Reach",
                    "Emberplateau Hollow"
            )),
            Map.entry("MOUNTAINS", List.of(
                    "Skyspire Peaks",
                    "Thundercrest Range",
                    "Stoneheart Ridge",
                    "Cloudpiercer Heights",
                    "Obsidian Summit",
                    "Frostfang Range",
                    "Eaglecrest Spires",
                    "Granitehigh Pass",
                    "Stormbrow Peaks",
                    "Ironclad Summit",
                    "Cragshadow Ridge",
                    "Snowmantle Peaks",
                    "Hightide Range",
                    "Crystalhorn Cliffs",
                    "Windshroud Peaks",
                    "Sapphire Ridge",
                    "Ghostpeak Range",
                    "Avalanche Crest",
                    "Boulderfall Heights",
                    "Mournrock Range"
            )),
            Map.entry("OCEAN", List.of(
                    "Saltfall Expanse",
                    "Sirens Reach",
                    "Foamcrest Shoals",
                    "Tidebound Deep",
                    "Pearlwater Trench",
                    "Coralsong Expanse",
                    "Tempest Gulf",
                    "Mooncurrent Shoals",
                    "Stormhelm Depths",
                    "Seabright Expanse",
                    "Driftwhisper Shoals",
                    "Azurecurl Basin",
                    "Shellwind Coast",
                    "Glimmersea Reach",
                    "Drownwave Deep",
                    "Horizonfall Waters",
                    "Bladewater Shoals",
                    "Kelpveil Reach",
                    "Tidecall Expanse",
                    "Ebbbright Deep"
            )),
            Map.entry("RIVER", List.of(
                    "Silvercurrent Vale",
                    "Mistglen Stream",
                    "Ripplebend",
                    "Moonbrook Run",
                    "Amberford River",
                    "Thornwater Glide",
                    "Windrush Channel",
                    "Frostbrook Reach",
                    "Willowflow Vale",
                    "Starlit Stream",
                    "Shimmerbank Run",
                    "Brambleford Crossing",
                    "Dewmire River",
                    "Glowwater Stream",
                    "Crystalford Vein",
                    "Meadowrun Vale",
                    "Glintbank Channel",
                    "Twilight Stream",
                    "Fernrush Curve",
                    "Silverline Run"
            )),
            Map.entry("BEACH", List.of(
                    "Seashell Strand",
                    "Tidewhisper Coast",
                    "Driftshore",
                    "Coralwind Bay",
                    "Surfside Barrow",
                    "Moonshore Flats",
                    "Saltbloom Strand",
                    "Sandlark Cove",
                    "Pebblefall Shore",
                    "Oceanmist Beach",
                    "Shellsong Strand",
                    "Dawnshore Reach",
                    "Brinecrest Beach",
                    "Gullwing Sands",
                    "Shellbreeze Shore",
                    "Highwater Strand",
                    "Sunspill Coast",
                    "Foamlight Beach",
                    "Azurestrand Cove",
                    "Baysong Beach"
            )),
            Map.entry("NETHER_WASTES", List.of(
                    "Ashen Wastes",
                    "Ember Hollow",
                    "Scorchbarrow",
                    "Cinderfield Rift",
                    "Blazeforge Flats",
                    "Sulfur Spire",
                    "Magmafell Basin",
                    "Charrock Expanse",
                    "Heatshroud Wastes",
                    "Flamewaste Reach",
                    "Pyrewind Fields",
                    "Burnscar Flats",
                    "Inferno Ridge",
                    "Smoldercrest Sands",
                    "Lavafall Hollow",
                    "Cinderveil Plains",
                    "Obsidian Rift",
                    "Scalding Glade",
                    "Sootspine Barrens",
                    "Flickerfall Basin"
            )),
            Map.entry("NETHER_CRIMSON", List.of(
                    "Bloodveil Thicket",
                    "Crimson Spire",
                    "Emberroot Grove",
                    "Scarlet Boughs",
                    "Redsunder Forest",
                    "Sanguine Hollow",
                    "Blazebramble Wood",
                    "Goreleaf Grove",
                    "Scarletwind Hollow",
                    "Cinderbloom Thicket",
                    "Bloodthorn Stand",
                    "Fiendwood Vale",
                    "Flamevine Grove",
                    "Scarletroot Coppice",
                    "Embershade Thicket",
                    "Razorthorn Wood",
                    "Ashenbloom Grove",
                    "Bloodroot Dell",
                    "Crimsonflame Wood",
                    "Hellbloom Thicket"
            )),
            Map.entry("NETHER_WARPED", List.of(
                    "Veilwood Twist",
                    "Warpshade Hollow",
                    "Neonspike Grove",
                    "Twistroot Wood",
                    "Sporeglow Thicket",
                    "Azurebloom Stand",
                    "Warpedfern Vale",
                    "Ghostbloom Hollow",
                    "Sorrowspore Grove",
                    "Nightshade Dell",
                    "Lumosheen Wood",
                    "Shattered Veil",
                    "Aurafungus Stand",
                    "Mistvine Hollow",
                    "Glowshard Grove",
                    "Shadowspore Thicket",
                    "Warpfall Dell",
                    "Fernflare Wood",
                    "Sporehollow Stand",
                    "Neonmist Grove"
            )),
            Map.entry("NETHER_BASALT", List.of(
                    "Basalt Spine",
                    "Obsidian Rift",
                    "Magmafall Chasm",
                    "Stoneforge Deep",
                    "Ashspire Gorge",
                    "Shadowstone Basin",
                    "Searridge Cliffs",
                    "Lavaflow Crags",
                    "Coalvein Hollow",
                    "Riftblack Vale",
                    "Flarestone Gorge",
                    "Glowing Basalt Crest",
                    "Charrock Hollow",
                    "Pyreledge Basin",
                    "Steamfall Ravine",
                    "Magmaforge Spire",
                    "Coalspine Chasm",
                    "Sootcliff Rift",
                    "Basaltfire Hollow",
                    "Obsidian Spine"
            )),
            Map.entry("NETHER_SOUL", List.of(
                    "Echoing Sands",
                    "Soulrift Wastes",
                    "Whisperdust Hollow",
                    "Spiritfall Fen",
                    "Soulshadow Flats",
                    "Ethereal Dunes",
                    "Gravelmourn Valley",
                    "Ashen Whisper Vale",
                    "Wailwind Expanse",
                    "Mournstone Flats",
                    "Palefire Marsh",
                    "Sorrowrift Hollow",
                    "Shadewind Dunes",
                    "Spectral Mire",
                    "Phantom Sands",
                    "Wraithglow Basin",
                    "Duskgloom Expanse",
                    "Frosted Soul Fen",
                    "Hollowwhisper Flats",
                    "Ebonwish Hollow"
            )),
            Map.entry("MUSHROOM", List.of(
                    "Glowcap Fields",
                    "Mushroom Knoll",
                    "Sporelight Dell",
                    "Fungi Bloom Grove",
                    "Mycelium Hollow",
                    "Capstone Rise",
                    "Mirecap Valley",
                    "Shimmering Caps",
                    "Luminous Grove",
                    "Sporebloom Flats",
                    "Mushlight Thicket",
                    "Velvetcap Hollow",
                    "Mooncap Cove",
                    "Fungal Fen",
                    "Glowroot Vale",
                    "Sporeshade Rise",
                    "Mushbloom Glade",
                    "Capveil Basin",
                    "Mycofall Hollow",
                    "Lumencap Plain"
            )),
            Map.entry("END", List.of(
                    "Starfall Expanse",
                    "Voidglow Isles",
                    "Aether Spire",
                    "Echoing Shardlands",
                    "Dreamlight Plateau",
                    "Astral Hollow",
                    "Enderwind Fields",
                    "Galexis Reach",
                    "Moonshard Vale",
                    "Luminous Rift",
                    "Silvershard Flats",
                    "Voidspire Expanse",
                    "Echoing Drift",
                    "Aetherstone Rise",
                    "Starlight Plain",
                    "Eternity Hollow",
                    "Nebulight Reach",
                    "Endfall Basin",
                    "Mirrored Vale",
                    "Astral Crest"
            ))
    );

    /**
     * Adjective/noun pools derived from BIOME_NAME_POOLS by splitting each two-word name on its
     * first space (e.g. "Elden Meadows" -> "Elden" + "Meadows"). Combining them independently turns
     * ~20 pre-written names per biome into up to size(adjectives) x size(nouns) unique combinations,
     * without authoring new content. Single-word entries (no space) are kept as standalone nouns.
     */
    private static final Map<String, List<String>> BIOME_ADJECTIVES = new HashMap<>();
    private static final Map<String, List<String>> BIOME_NOUNS = new HashMap<>();

    static {
        for (Map.Entry<String, List<String>> entry : BIOME_NAME_POOLS.entrySet()) {
            List<String> adjectives = new ArrayList<>();
            List<String> nouns = new ArrayList<>();
            for (String name : entry.getValue()) {
                int spaceIndex = name.indexOf(' ');
                if (spaceIndex > 0) {
                    adjectives.add(name.substring(0, spaceIndex));
                    nouns.add(name.substring(spaceIndex + 1));
                } else {
                    nouns.add(name);
                }
            }
            BIOME_ADJECTIVES.put(entry.getKey(), adjectives.isEmpty() ? List.of("Wandering") : adjectives);
            BIOME_NOUNS.put(entry.getKey(), nouns);
        }
    }

    private static final Map<String, String> BIOME_SUBTITLES = Map.ofEntries(
            Map.entry("PLAINS", "Where the wind whispers secrets..."),
            Map.entry("FOREST", "The canopy hides old stories..."),
            Map.entry("DARK_FOREST", "Shadows move with intent here..."),
            Map.entry("TAIGA", "The cold breathes a cruel warning..."),
            Map.entry("SNOWY_TAIGA", "Frost does not forget your name..."),
            Map.entry("JUNGLE", "Life entangles every step..."),
            Map.entry("SWAMP", "Mire and mist claim the unwary..."),
            Map.entry("DESERT", "The sun remembers every traveler..."),
            Map.entry("BADLANDS", "The earth is broken and beautiful..."),
            Map.entry("MOUNTAINS", "Peaks pierce the sky with silent fury..."),
            Map.entry("OCEAN", "Tides carry secrets older than time..."),
            Map.entry("RIVER", "The current whispers ancient paths..."),
            Map.entry("BEACH", "Salt and sand mark a border of worlds..."),
            Map.entry("NETHER_WASTES", "Fire and ash shape every horizon..."),
            Map.entry("NETHER_CRIMSON", "The crimson growth feeds on heat and hunger..."),
            Map.entry("NETHER_WARPED", "A strange calm flickers among the spores..."),
            Map.entry("NETHER_BASALT", "Black stone and lava forge an odd beauty..."),
            Map.entry("NETHER_SOUL", "Whispers drift through the cold ash..."),
            Map.entry("MUSHROOM", "Glow and fungus shape a quiet realm..."),
            Map.entry("END", "Stars themselves seem closer here...")
    );

    private static final Map<String, List<String>> BIOME_FLAVOR_TEXTS = Map.ofEntries(
            Map.entry("PLAINS", List.of("A familiar hush falls over the fields.", "The breeze carries distant echoes.")),
            Map.entry("FOREST", List.of("Leaves rustle like old voices.", "Shade and light play tricks on the mind.")),
            Map.entry("DARK_FOREST", List.of("The woods do not like being watched.", "You can almost hear the trees breathe.")),
            Map.entry("TAIGA", List.of("The pine air tastes of ice.", "Footsteps vanish in the snow.")),
            Map.entry("SNOWY_TAIGA", List.of("The cold is a constant companion.", "Each breath turns to silver mist.")),
            Map.entry("JUNGLE", List.of("The canopy hums with restless life.", "Every vine seems to follow you.")),
            Map.entry("SWAMP", List.of("The fog seems to have its own heartbeat.", "The water glows with a strange light.")),
            Map.entry("DESERT", List.of("Heat shimmers in every direction.", "The sand hides more than it reveals.")),
            Map.entry("BADLANDS", List.of("The ground looks older than memory.", "Stone and dust form jagged poetry.")),
            Map.entry("MOUNTAINS", List.of("Sky and stone meet in quiet grandeur.", "Wind sharpens every thought.")),
            Map.entry("OCEAN", List.of("Salt and spray mark a wild boundary.", "The horizon seems endless here.")),
            Map.entry("RIVER", List.of("The water remembers every path.", "Current and stone trade soft whispers.")),
            Map.entry("BEACH", List.of("Waves sing a slow, steady song.", "Sand shifts like a sleeping beast.")),
            Map.entry("NETHER_WASTES", List.of("Ash drifts like a slow storm.", "Heat and shadow fight for space.")),
            Map.entry("NETHER_CRIMSON", List.of("Red light bathes the world in hunger.", "The air feels thick and restless.")),
            Map.entry("NETHER_WARPED", List.of("Colors bleed into unnatural calm.", "Spore stalks sway without wind.")),
            Map.entry("NETHER_BASALT", List.of("Stone groans under the heat.", "Lava light paints everything red.")),
            Map.entry("NETHER_SOUL", List.of("Cold ash whispers in broken tongues.", "A faint blue glow haunts the air.")),
            Map.entry("MUSHROOM", List.of("The ground seems to glow from within.", "The air smells sweet and strange.")),
            Map.entry("END", List.of("Silence falls heavily here.", "The stars feel within reach."))
    );

    public ZoneManager(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    public void handlePlayerZone(Player player) {
        if (player == null) {
            return;
        }

        UUID id = player.getUniqueId();
        String currentZone = getCurrentZone(player);
        String previousZone = lastZones.get(id);

        if (currentZone == null) {
            lastZones.remove(id);
            return;
        }

        if (currentZone.equals(previousZone)) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfigManager().getConfigValues().getLong("settings.zone_change_cooldown", 45L) * 1000L;
        if (cooldowns.containsKey(id) && now - cooldowns.get(id) < cooldown) {
            return;
        }

        cooldowns.put(id, now);
        lastZones.put(id, currentZone);
        plugin.getPlayerStatsService().recordZoneChange(player);
        sendZoneFeedback(player, currentZone);
    }

    /** Read-only lookup of the player's current zone display name (no cooldown/journal side effects). */
    public String getCurrentZoneDisplayName(Player player) {
        String currentZone = getCurrentZone(player);
        if (currentZone == null) {
            return "Unknown";
        }

        var section = plugin.getConfigManager().getZones().getConfigurationSection("zones." + currentZone);
        if (section != null) {
            String title = section.getString("title", currentZone);
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', title));
        }
        return getDynamicZoneTitle(player, currentZone);
    }

    private String getCurrentZone(Player player) {
        String currentBiome = player.getLocation().getBlock().getBiome().name().toUpperCase(Locale.ROOT);
        String biomeGroup = normalizeBiomeGroup(currentBiome);

        var zonesSection = plugin.getConfigManager().getZones().getConfigurationSection("zones");
        if (zonesSection != null) {
            for (String zoneName : zonesSection.getKeys(false)) {
                var section = plugin.getConfigManager().getZones().getConfigurationSection("zones." + zoneName);
                if (section == null) {
                    continue;
                }

                String type = section.getString("type", "BIOME");
                String id = section.getString("id");
                if (id == null) {
                    continue;
                }

                if ("BIOME".equalsIgnoreCase(type) && isBiomeMatch(id, currentBiome)) {
                    return zoneName;
                }
                if ("WORLDGUARD".equalsIgnoreCase(type) && plugin.isInsideWorldGuardRegion(player.getLocation(), id)) {
                    return zoneName;
                }
            }
        }

        int regionX = Math.floorDiv(player.getLocation().getBlockX(), ZONE_SIZE);
        int regionZ = Math.floorDiv(player.getLocation().getBlockZ(), ZONE_SIZE);
        String worldName = player.getWorld().getName();
        return String.format("%s|%s|%s|%d|%d", DYNAMIC_ZONE_PREFIX, biomeGroup, worldName, regionX, regionZ);
    }

    private void spawnZoneParticles(Player player, String particleName) {
        if (particleName == null || particleName.isBlank() || "NONE".equalsIgnoreCase(particleName)) {
            return;
        }
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.02);
        } catch (IllegalArgumentException ex) {
            // Unknown particle name in zones.yml — skip silently rather than spam the console on every zone entry.
        }
    }

    private boolean isBiomeMatch(String configuredId, String currentBiome) {
        String normalizedConfigured = configuredId.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (normalizedConfigured.equals(currentBiome)) {
            return true;
        }
        String normalizedCurrent = currentBiome.replace(' ', '_');
        return normalizedConfigured.contains(normalizedCurrent) || normalizedCurrent.contains(normalizedConfigured);
    }

    private void sendZoneFeedback(Player player, String zoneName) {
        var section = plugin.getConfigManager().getZones().getConfigurationSection("zones." + zoneName);
        String titleText;
        String subtitleText;
        String sound;
        String flavorLine;

        if (section != null) {
            titleText = section.getString("title", "");
            subtitleText = section.getString("subtitle", "");
            sound = section.getString("sound", DEFAULT_SOUND);
            List<String> flavorTexts = section.getStringList("flavor_texts");
            flavorLine = flavorTexts.isEmpty()
                    ? "A new zone awakens your imagination."
                    : flavorTexts.get(0);
        } else {
            titleText = getDynamicZoneTitle(player, zoneName);
            subtitleText = getDynamicZoneSubtitle(player);
            sound = DEFAULT_SOUND;
            flavorLine = getDynamicZoneFlavorText(player);
        }

        String legacyTitle = ChatColor.translateAlternateColorCodes('&', titleText);
        String legacySubtitle = ChatColor.translateAlternateColorCodes('&', subtitleText);
        player.sendTitle(legacyTitle, legacySubtitle, 10, 40, 10);
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);

        if (section != null) {
            spawnZoneParticles(player, section.getString("particles", "NONE"));
        }

        if (!titleText.isBlank()) {
            plugin.getPlayerJournalService().addEntry(player, "Arrived at " + legacyTitle + ChatColor.RESET + ".");
        }

        Component finalFlavor = Component.text("[RPGMood] ")
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(flavorLine));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                player.sendMessage(finalFlavor);
            }
        }.runTaskLater(plugin, 20L);
    }

    private String getDynamicZoneTitle(Player player, String zoneName) {
        return assignedZoneNames.computeIfAbsent(zoneName, key -> createUniqueZoneName(player, key));
    }

    private String createUniqueZoneName(Player player, String zoneName) {
        String biomeGroup = normalizeBiomeGroup(player.getLocation().getBlock().getBiome().name().toUpperCase(Locale.ROOT));
        List<String> adjectives = BIOME_ADJECTIVES.getOrDefault(biomeGroup, BIOME_ADJECTIVES.get("PLAINS"));
        List<String> nouns = BIOME_NOUNS.getOrDefault(biomeGroup, BIOME_NOUNS.get("PLAINS"));

        int seed = Math.abs(zoneName.hashCode());
        int totalCombinations = adjectives.size() * nouns.size();

        for (int attempt = 0; attempt < totalCombinations; attempt++) {
            int combinedIndex = (seed + attempt) % totalCombinations;
            String candidate = adjectives.get(combinedIndex / nouns.size()) + " " + nouns.get(combinedIndex % nouns.size());
            if (!assignedZoneNames.containsValue(candidate)) {
                return candidate;
            }
        }

        // Every adjective/noun combination for this biome is already in use somewhere on the server — fall
        // back to a numeric suffix rather than looping forever.
        String baseName = adjectives.get(seed % adjectives.size()) + " " + nouns.get(seed % nouns.size());
        String candidate = baseName;
        int suffix = 2;
        while (assignedZoneNames.containsValue(candidate)) {
            candidate = baseName + " " + suffix;
            suffix++;
        }
        return candidate;
    }

    /** Releases per-player tracking state; call on PlayerQuitEvent to avoid unbounded growth. */
    public void handlePlayerQuit(Player player) {
        UUID id = player.getUniqueId();
        cooldowns.remove(id);
        lastZones.remove(id);
    }

    private String getDynamicZoneSubtitle(Player player) {
        String biomeGroup = normalizeBiomeGroup(player.getLocation().getBlock().getBiome().name().toUpperCase(Locale.ROOT));
        return BIOME_SUBTITLES.getOrDefault(biomeGroup, "A strange new land stretches forth...");
    }

    private String getDynamicZoneFlavorText(Player player) {
        String biomeGroup = normalizeBiomeGroup(player.getLocation().getBlock().getBiome().name().toUpperCase(Locale.ROOT));
        List<String> flavorTexts = BIOME_FLAVOR_TEXTS.getOrDefault(biomeGroup, List.of("A new zone awakens your imagination."));
        int index = Math.abs(player.getLocation().hashCode()) % flavorTexts.size();
        return flavorTexts.get(index);
    }

    private String normalizeBiomeGroup(String biomeKey) {
        return BIOME_GROUP.getOrDefault(biomeKey, biomeKey);
    }
}
