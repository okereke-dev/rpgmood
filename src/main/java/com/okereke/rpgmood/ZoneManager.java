package com.okereke.rpgmood;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class ZoneManager {

    /** Prefix for zone keys resolved by ZoneClusterService (organic, persisted zone clusters) rather than curated zones.yml entries. */
    public static final String CLUSTER_ZONE_PREFIX = "CLUSTER_ZONE";
    private static final String DEFAULT_SOUND = "ambient.weather.wind_light";

    /** How long a player must stay in a new zone before feedback fires — filters out border pacing and fast pass-throughs. */
    private static final long NORMAL_DWELL_MILLIS = 1500L;
    /** Longer dwell time while gliding/riding a vehicle, where fast traversal makes brief zone crossings even more likely. */
    private static final long FAST_DWELL_MILLIS = 3000L;
    /** Minimum time between showing the big Title/Subtitle for the *same* zone again, independent of the ambient cooldown. */
    private static final long TITLE_SUPPRESS_MILLIS = 5 * 60 * 1000L;
    private static final int MAX_RECENT_TITLE_ZONES_PER_PLAYER = 20;

    private final RPGMoodPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, String> lastZones = new HashMap<>();
    private final Map<UUID, String> pendingZones = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> pendingTasks = new HashMap<>();
    private final Map<UUID, LinkedHashMap<String, Long>> recentTitleShownAt = new HashMap<>();

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

    /**
     * A third, independently-authored word bank per biome group — a middle "descriptor" segment
     * combined with BIOME_ADJECTIVES/BIOME_NOUNS to build 3-part zone names (e.g. "Whispering
     * Ashen Hollow") instead of 2-part ones. Unlike those two maps, these are hand-written
     * directly rather than split out of BIOME_NAME_POOLS — adding one ~15-word list here
     * multiplies the combinatorial name space for a group far more than doubling either existing
     * list would (adjectives x descriptors x nouns instead of just adjectives x nouns).
     */
    private static final Map<String, List<String>> BIOME_DESCRIPTORS = Map.ofEntries(
            Map.entry("PLAINS", List.of("Sunlit", "Windswept", "Golden", "Dew-Kissed", "Wheatstrewn", "Meadow-Bright", "Breezy", "Open-Sky", "Wildflower", "Amber-Lit", "Rolling", "Sunwarmed", "Grassy", "Wind-Carved", "Honeytouched", "Pastoral")),
            Map.entry("FOREST", List.of("Mossy", "Shaded", "Leaf-Strewn", "Ancient", "Fern-Choked", "Timberdeep", "Sunlit-Canopy", "Root-Bound", "Whispering", "Emerald", "Bark-Scarred", "Deep-Rooted", "Overgrown", "Verdant", "Wild", "Hollow-Touched")),
            Map.entry("DARK_FOREST", List.of("Shadow-Cloaked", "Grim", "Twisted", "Nightbound", "Thornveiled", "Moonless", "Grave", "Hollow", "Sinister", "Witchbound", "Bramble-Choked", "Silent", "Fell", "Umbral", "Dread-Touched", "Grim-Rooted")),
            Map.entry("TAIGA", List.of("Frost-Kissed", "Pine-Cloaked", "Boreal", "Snowlit", "Icebound", "Windbitten", "Silver-Frosted", "Coldwrought", "Timbered", "Winterbound", "Rime-Touched", "Frosty", "Needle-Strewn", "Shivering", "Hoarfrost", "Northwind")),
            Map.entry("SNOWY_TAIGA", List.of("Frozen", "Glacial", "Ice-Veiled", "Blizzardswept", "Snowbound", "Frostbitten", "Hollow-White", "Pale", "Winterfrost", "Icewrought", "Whiteout", "Frost-Locked", "Arctic", "Rime-Bound", "Snowdrift", "Numbing")),
            Map.entry("JUNGLE", List.of("Vine-Choked", "Lush", "Humid", "Canopy-Veiled", "Overgrown", "Mist-Shrouded", "Emerald-Deep", "Wild", "Fern-Tangled", "Sweltering", "Rootbound", "Verdant-Thick", "Untamed", "Bloomheavy", "Moss-Draped", "Rainsoaked")),
            Map.entry("SWAMP", List.of("Mire-Sunk", "Murky", "Fog-Bound", "Rot-Touched", "Bogsoaked", "Reed-Choked", "Marshbound", "Stagnant", "Mudslick", "Mosswater", "Gloomy", "Fen-Deep", "Waterlogged", "Peat-Dark", "Brackish", "Damp")),
            Map.entry("DESERT", List.of("Sun-Scorched", "Windblasted", "Duststrewn", "Mirage-Haunted", "Sandbound", "Blazing", "Dunewrought", "Cracked", "Arid", "Heatworn", "Sunbaked", "Bonedry", "Scoured", "Glassbound", "Shimmering", "Windswept")),
            Map.entry("BADLANDS", List.of("Ironscar", "Sundered", "Cracked-Earth", "Rustbound", "Copperveined", "Ochre-Streaked", "Wind-Carved", "Sunburnt", "Stonebroken", "Claybound", "Dustworn", "Redrock", "Weathered", "Scoured", "Ashenred", "Barren")),
            Map.entry("MOUNTAINS", List.of("Stormcrest", "Windbitten", "Skyhigh", "Cragbound", "Stonewrought", "Cloudveiled", "Granite-Bound", "Steep", "Rugged", "Frostpeaked", "Boulderstrewn", "Windswept", "Echoing", "Precipice-Bound", "Sheer", "Thundercrowned")),
            Map.entry("OCEAN", List.of("Tideborn", "Salt-Kissed", "Deepbound", "Wavecrest", "Foamtouched", "Current-Swept", "Abyssal", "Driftbound", "Stormtossed", "Coralbound", "Brinewrought", "Sunkissed", "Depthless", "Glimmering", "Tidal", "Windswept")),
            Map.entry("RIVER", List.of("Silverflow", "Meander-Bound", "Rushing", "Bankworn", "Riffled", "Streambound", "Winding", "Clearwater", "Fastflow", "Mossbank", "Rapid-Carved", "Gentle", "Ripplebound", "Freshbound", "Stonebed", "Wandering")),
            Map.entry("BEACH", List.of("Tideswept", "Sunwashed", "Shellbound", "Driftwood-Strewn", "Foamkissed", "Windblown", "Sandworn", "Coastbound", "Salt-Touched", "Sunbleached", "Waveworn", "Glimmering", "Shorebound", "Breezy", "Golden-Sand", "Tidal")),
            Map.entry("NETHER_WASTES", List.of("Ashbound", "Emberlit", "Scorchbound", "Sulfurwreathed", "Cinderstrewn", "Fireborn", "Bleak", "Charwrought", "Smolderbound", "Heatworn", "Ruinbound", "Ashen", "Firelit", "Blistered", "Wastebound", "Grim")),
            Map.entry("NETHER_CRIMSON", List.of("Bloodlit", "Fungal-Veiled", "Crimsonbound", "Fleshbound", "Pulsating", "Warped-Red", "Hungerbound", "Bloomdark", "Vein-Wrought", "Sanguine", "Growthbound", "Scarletlit", "Thornveiled", "Wetbound", "Feral", "Rawbound")),
            Map.entry("NETHER_WARPED", List.of("Sporeveiled", "Glowbound", "Eerielit", "Fungal-Touched", "Twistbound", "Uncanny", "Ghostlit", "Neonveiled", "Silent", "Hazebound", "Strangelit", "Mistwarped", "Cyanbound", "Hollowlit", "Driftbound", "Otherworldly")),
            Map.entry("NETHER_BASALT", List.of("Ashen-Stone", "Obsidianbound", "Steamveiled", "Cragbound", "Charcoal", "Smolderstone", "Blackrock", "Fissurebound", "Heatwrought", "Stonewreathed", "Sulfurbound", "Rockscarred", "Cindercrowned", "Grim-Stone", "Basaltic", "Chasmbound")),
            Map.entry("NETHER_SOUL", List.of("Whisperbound", "Ashen-Pale", "Soulbound", "Hollowlit", "Duskveiled", "Wraithbound", "Coldash", "Sorrowbound", "Palewreathed", "Echobound", "Grievebound", "Mournlit", "Ghostveiled", "Duststrewn", "Spectral", "Hushbound")),
            Map.entry("MUSHROOM", List.of("Glowcapped", "Sporeveiled", "Fungal-Bright", "Mistbound", "Luminous", "Mycelbound", "Softlit", "Dreambound", "Gentlebloom", "Twilightlit", "Mossbright", "Whimsical", "Glowbound", "Softbloom", "Hazelit", "Wonderbound")),
            Map.entry("END", List.of("Starbound", "Voidtouched", "Silent", "Etherbound", "Cosmic", "Driftbound", "Shardlit", "Eternal", "Hollowbound", "Astral", "Farbound", "Echobound", "Dreamlit", "Endless", "Stillbound", "Twilit"))
    );

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
            cancelPending(id);
            return;
        }

        if (currentZone.equals(previousZone)) {
            cancelPending(id);
            return;
        }

        if (currentZone.equals(pendingZones.get(id))) {
            return;
        }

        cancelPending(id);
        pendingZones.put(id, currentZone);

        long dwellMillis = isFastMoving(player) ? FAST_DWELL_MILLIS : NORMAL_DWELL_MILLIS;
        org.bukkit.scheduler.BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                pendingTasks.remove(id);
                String pending = pendingZones.remove(id);
                if (pending == null || !player.isOnline()) {
                    return;
                }
                if (!pending.equals(getCurrentZone(player))) {
                    return;
                }
                confirmZoneChange(player, pending);
            }
        }.runTaskLater(plugin, dwellMillis / 50L);
        pendingTasks.put(id, task);
    }

    private void cancelPending(UUID id) {
        org.bukkit.scheduler.BukkitTask task = pendingTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        pendingZones.remove(id);
    }

    private boolean isFastMoving(Player player) {
        return player.isGliding() || player.getVehicle() != null;
    }

    private void confirmZoneChange(Player player, String currentZone) {
        UUID id = player.getUniqueId();

        String previousZone = lastZones.get(id);
        String previousDisplay = previousZone != null ? getZoneDisplayName(previousZone, player) : "The Unknown";

        if (!plugin.getConfigManager().getConfigValues().getBoolean("player_effects." + id, true)) {
            lastZones.put(id, currentZone);
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
        plugin.getAchievementManager().onZoneVisited(player, currentZone);

        // Fire API event
        boolean isDynamic = currentZone.startsWith(CLUSTER_ZONE_PREFIX);
        String newDisplay = getZoneDisplayName(currentZone, player);
        com.okereke.rpgmood.api.PlayerZoneChangeEvent event = new com.okereke.rpgmood.api.PlayerZoneChangeEvent(
                player, previousZone, currentZone, previousDisplay, newDisplay, isDynamic);
        Bukkit.getPluginManager().callEvent(event);

        boolean showTitle = shouldShowTitle(player, currentZone, now);
        sendZoneFeedback(player, currentZone, showTitle);

        boolean wasNewDiscovery = plugin.getZoneDiscoveryService().getDiscoveries(player).stream()
                .noneMatch(z -> z.key().equals(currentZone));
        plugin.getZoneDiscoveryService().recordDiscovery(player, currentZone, newDisplay);
        if (wasNewDiscovery) {
            plugin.getPlayerLevelService().addXp(player, 15L);
        }
        plugin.getZoneScoreboardService().updateScoreboard(player);
    }

    /** Gets the display name for a zone key. */
    private String getZoneDisplayName(String zoneKey, Player player) {
        if (zoneKey.startsWith(CLUSTER_ZONE_PREFIX)) {
            return plugin.getZoneClusterService().getDisplayName(zoneKey);
        }
        return zoneKey;
    }

    /** Gates the big Title/Subtitle behind its own per-player opt-out and a longer "seen recently" memory than the ambient cooldown. */
    private boolean shouldShowTitle(Player player, String zoneName, long now) {
        UUID id = player.getUniqueId();
        if (!plugin.getConfigManager().getConfigValues().getBoolean("player_titles." + id, true)) {
            return false;
        }

        LinkedHashMap<String, Long> recent = recentTitleShownAt.computeIfAbsent(id, k -> new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > MAX_RECENT_TITLE_ZONES_PER_PLAYER;
            }
        });

        Long last = recent.get(zoneName);
        boolean show = last == null || (now - last) >= TITLE_SUPPRESS_MILLIS;
        if (show) {
            recent.put(zoneName, now);
        }
        return show;
    }

    /** Read-only lookup of the player's current zone display name (no cooldown/journal side effects). */
    public String getCurrentZoneDisplayName(Player player) {
        String currentZone = getCurrentZone(player);
        if (currentZone == null) {
            return "Unknown";
        }

        if (currentZone.startsWith(CLUSTER_ZONE_PREFIX)) {
            return plugin.getZoneClusterService().getDisplayName(currentZone);
        }
        return currentZone;
    }

    private String getCurrentZone(Player player) {
        String clusterZone = plugin.getZoneClusterService().resolveClusterZone(player);
        if (clusterZone != null) {
            return clusterZone;
        }
        // Cluster creation was throttled this tick — hold the player's last confirmed (or
        // still-pending) zone rather than resolving nothing; equal to the previous value makes
        // handlePlayerZone's own first check a no-op, so no zone-change fires from this.
        UUID heldId = player.getUniqueId();
        String held = lastZones.get(heldId);
        return held != null ? held : pendingZones.get(heldId);
    }

    /**
     * Sends zone-entry feedback entirely through Title/Subtitle + sound/particles — no action bar.
     * The subtitle is picked (per-visit, deterministically by location) from a pool combining the
     * zone's configured {@code subtitle} with its {@code flavor_texts}, so the variety those extra
     * lines were meant to provide actually shows up, and the whole thing rides one channel that
     * nothing else in the plugin ever contends for.
     */
    private void sendZoneFeedback(Player player, String zoneName, boolean showTitle) {
        String titleText;
        String subtitleText;
        String sound;

        if (zoneName.startsWith(CLUSTER_ZONE_PREFIX)) {
            titleText = plugin.getZoneClusterService().getDisplayName(zoneName);
            sound = DEFAULT_SOUND;

            String flavorGroup = plugin.getZoneClusterService().getFlavorGroup(zoneName);
            List<String> subtitlePool = new ArrayList<>();
            subtitlePool.add(BIOME_SUBTITLES.getOrDefault(flavorGroup, "A strange new land stretches forth..."));
            subtitlePool.addAll(BIOME_FLAVOR_TEXTS.getOrDefault(flavorGroup, List.of()));
            subtitleText = pickSubtitle(player, subtitlePool);
        } else {
            titleText = zoneName;
            sound = DEFAULT_SOUND;
            subtitleText = pickSubtitle(player, List.of());
        }

        String strippedTitle = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', titleText));
        String legacyTitle = ChatColor.translateAlternateColorCodes('&', getZoneDangerColorCode(player) + strippedTitle);
        String legacySubtitle = ChatColor.translateAlternateColorCodes('&', subtitleText);

        if (showTitle && !titleText.isBlank()) {
            if (isFastMoving(player)) {
                player.sendTitle(legacyTitle, legacySubtitle, 5, 20, 5);
            } else {
                player.sendTitle(legacyTitle, legacySubtitle, 10, 40, 10);
            }
        }
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);

        if (!titleText.isBlank()) {
            plugin.getPlayerJournalService().addEntry(player, "Arrived at " + legacyTitle + ChatColor.RESET + ".");
        }
    }

    /**
     * Colors a zone title by local danger rather than a hand-picked-per-zone code, reusing the
     * same tier palette RPGLoot uses for item rarity so zones/mobs/loot all read as one visual
     * language: gray → yellow → purple → green → gold as the Zombie-equivalent level rises.
     * Applies to every zone, curated or dynamically generated — this API only supports the
     * legacy 16-color codes, so the tiers use the closest (or exact, for 4 of 5) legacy match
     * to RPGLoot's actual {@code Rarity} colors.
     */
    String getZoneDangerColorCode(Player player) {
        if (plugin.getMobScalingService() == null) {
            return "&f";
        }
        int baseLevel = plugin.getMobScalingService().getBaseLevel(EntityType.ZOMBIE);
        int level = plugin.getMobScalingService().calculateLevelAt(player.getLocation(), baseLevel);
        if (level <= 3) return "&7";  // Common
        if (level <= 9) return "&e";  // Uncommon
        if (level <= 17) return "&5"; // Rare (exact match to RPGLoot's #AA00AA)
        if (level <= 27) return "&a"; // Hero
        return "&6";                  // Legendary
    }

    /** Deterministically picks one line from a pool of subtitle/flavor candidates, varying by location so repeat visits don't always show the same line. */
    private String pickSubtitle(Player player, List<String> candidates) {
        if (candidates.isEmpty()) {
            return "A new zone awakens your imagination.";
        }
        int index = Math.abs(player.getLocation().hashCode()) % candidates.size();
        return candidates.get(index);
    }

    /**
     * Deterministic adjective/descriptor/noun name generator, seeded by a stable string, scoped
     * to a biome group. {@code isTaken} is the uniqueness oracle — callers own where "already
     * assigned" state lives (ZoneClusterService keeps a full, non-evicting name set for
     * persisted clusters, since their names are permanent) rather than this method dictating it.
     * Tries the full 3-way combinatorial space first (adjective x descriptor x noun — see
     * BIOME_DESCRIPTORS), falls back to a numeric suffix if the whole space is exhausted.
     */
    public String createUniqueZoneName(String biomeGroup, String seedKey, Predicate<String> isTaken) {
        List<String> adjectives = BIOME_ADJECTIVES.getOrDefault(biomeGroup, BIOME_ADJECTIVES.get("PLAINS"));
        List<String> descriptors = BIOME_DESCRIPTORS.getOrDefault(biomeGroup, BIOME_DESCRIPTORS.get("PLAINS"));
        List<String> nouns = BIOME_NOUNS.getOrDefault(biomeGroup, BIOME_NOUNS.get("PLAINS"));

        int seed = Math.abs(seedKey.hashCode());
        int descriptorNounCombos = descriptors.size() * nouns.size();
        int totalCombinations = adjectives.size() * descriptorNounCombos;

        for (int attempt = 0; attempt < totalCombinations; attempt++) {
            int combinedIndex = (seed + attempt) % totalCombinations;
            int adjIndex = combinedIndex / descriptorNounCombos;
            int remainder = combinedIndex % descriptorNounCombos;
            int descIndex = remainder / nouns.size();
            int nounIndex = remainder % nouns.size();
            String candidate = adjectives.get(adjIndex) + " " + descriptors.get(descIndex) + " " + nouns.get(nounIndex);
            if (!isTaken.test(candidate)) {
                return candidate;
            }
        }

        // The entire adjective x descriptor x noun space for this biome is already in use
        // somewhere on the server — fall back to a numeric suffix rather than looping forever.
        String baseName = adjectives.get(seed % adjectives.size()) + " "
                + descriptors.get(seed % descriptors.size()) + " "
                + nouns.get(seed % nouns.size());
        String candidate = baseName;
        int suffix = 2;
        while (isTaken.test(candidate)) {
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
        recentTitleShownAt.remove(id);
        cancelPending(id);
    }

    public static String normalizeBiomeGroup(String biomeKey) {
        return BIOME_GROUP.getOrDefault(biomeKey, biomeKey);
    }

    // Climate thresholds for resolveNamingBiomeGroup. 0.15 matches vanilla's own rain/snow
    // cutoff; the other three are judgment calls — retune here if a biome ends up feeling
    // miscategorized in practice.
    private static final double CLIMATE_FROZEN_MAX_TEMP = 0.15;
    private static final double CLIMATE_MILD_MAX_TEMP = 0.5;
    private static final double CLIMATE_HOT_MIN_TEMP = 1.0;
    private static final double CLIMATE_WET_MIN_HUMIDITY = 0.35;

    /**
     * Resolves which existing name-pool to borrow from for a biome with no dedicated pool of its
     * own — either a vanilla variant never added to BIOME_GROUP (Savanna, Windswept Hills
     * family, Cherry Grove, Wooded Badlands, Sparse Jungle, deep ocean variants, etc. all hit
     * this today) or a datapack/mod biome RPGMood has never seen. Checked in order: (1) does the
     * biome already have a dedicated pool via BIOME_GROUP? use it, zero change from today's
     * behavior. (2) obvious water-biome keyword in the raw name? borrow that pool. (3) bucket by
     * real temperature/humidity (RegionAccessor#getTemperature/getHumidity) into the
     * closest-themed existing pool — never a new pool, just an existing one that actually fits
     * the biome's climate instead of always silently defaulting to Plains.
     */
    public static String resolveNamingBiomeGroup(String rawBiomeName, double temperature, double humidity) {
        String group = normalizeBiomeGroup(rawBiomeName);
        if (BIOME_ADJECTIVES.containsKey(group)) {
            return group;
        }
        if (rawBiomeName.contains("OCEAN")) {
            return "OCEAN";
        }
        if (rawBiomeName.contains("RIVER")) {
            return "RIVER";
        }
        if (rawBiomeName.contains("BEACH") || rawBiomeName.contains("SHORE")) {
            return "BEACH";
        }

        boolean wet = humidity >= CLIMATE_WET_MIN_HUMIDITY;
        if (temperature < CLIMATE_FROZEN_MAX_TEMP) {
            return "SNOWY_TAIGA";
        }
        if (temperature < CLIMATE_MILD_MAX_TEMP) {
            return wet ? "TAIGA" : "MOUNTAINS";
        }
        if (temperature < CLIMATE_HOT_MIN_TEMP) {
            return wet ? "FOREST" : "PLAINS";
        }
        return wet ? "JUNGLE" : "DESERT";
    }
}
