package com.okereke.rpgmood.mob;

import com.okereke.rpgmood.RPGMoodPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rolls and applies RPG-flavored affixes onto freshly-scaled mobs — some (Swift, Wraith,
 * Regenerating) are self-applied at spawn via attributes/potion effects; the on-hit ones
 * (Bleeding, Poisonous, Chilling) are only tagged here and actually inflicted by
 * {@link MobAffixCombatListener} when the mob hits a player. Self-contained: no dependency on
 * RPGLoot, even though the reversed on-player Bleeding DOT mirrors the shape of RPGLoot's own
 * player-applies-bleed-to-mob mechanic.
 */
public class MobAffixService {

    private final RPGMoodPlugin plugin;
    private final NamespacedKey affixesKey;
    private final NamespacedKey swiftSpeedKey;
    private final NamespacedKey regenToughnessKey;
    private final Random random = new Random();

    public MobAffixService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.affixesKey = new NamespacedKey(plugin, "affixes");
        this.swiftSpeedKey = new NamespacedKey(plugin, "affix_swift");
        this.regenToughnessKey = new NamespacedKey(plugin, "affix_regen_toughness");
    }

    public NamespacedKey getAffixesKey() {
        return affixesKey;
    }

    /** True if this mob was rolled with the given affix. */
    public boolean hasAffix(LivingEntity entity, MobAffix affix) {
        String tag = entity.getPersistentDataContainer().get(affixesKey, PersistentDataType.STRING);
        if (tag == null || tag.isBlank()) return false;
        for (String name : tag.split(",")) {
            if (name.equals(affix.name())) return true;
        }
        return false;
    }

    /** Rolls 0-2 affixes for a freshly-scaled mob, weighted by its level, and applies their effects. */
    public void rollAndApply(LivingEntity entity, int level) {
        if (entity == null || entity.getScoreboardTags().contains("rpgmood_affixed")) return;
        entity.addScoreboardTag("rpgmood_affixed");

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.affixes");
        if (section == null || !section.getBoolean("enabled", true)) return;

        if (random.nextDouble() >= chanceForLevel(section, "chance-by-level", level)) return;

        List<MobAffix> pool = new ArrayList<>(List.of(MobAffix.values()));
        List<MobAffix> rolled = new ArrayList<>();

        // Autumn: elevated chance the first affix is Swift (Hunt season).
        double autumnSwift = plugin.getAdvancedAiService() != null
                ? plugin.getAdvancedAiService().getAutumnSwiftBonus() : 0.0;
        if (autumnSwift > 0 && pool.contains(MobAffix.SWIFT) && random.nextDouble() < autumnSwift) {
            pool.remove(MobAffix.SWIFT);
            rolled.add(MobAffix.SWIFT);
        } else {
            rolled.add(pool.remove(random.nextInt(pool.size())));
        }

        int maxPerMob = section.getInt("max-per-mob", 2);
        if (maxPerMob >= 2 && random.nextDouble() < chanceForLevel(section, "second-affix-chance-by-level", level)) {
            rolled.add(pool.remove(random.nextInt(pool.size())));
        }

        for (MobAffix affix : rolled) {
            applySelf(entity, affix, level);
        }

        StringBuilder tag = new StringBuilder();
        for (MobAffix affix : rolled) {
            if (tag.length() > 0) tag.append(',');
            tag.append(affix.name());
        }
        entity.getPersistentDataContainer().set(affixesKey, PersistentDataType.STRING, tag.toString());

        String currentName = entity.getCustomName();
        if (currentName != null) {
            StringBuilder suffix = new StringBuilder(" §7[");
            for (int i = 0; i < rolled.size(); i++) {
                if (i > 0) suffix.append(", ");
                suffix.append(rolled.get(i).getDisplayName());
            }
            suffix.append(']');
            entity.setCustomName(currentName + suffix);
        }
    }

    /** Highest chance-band threshold at or below the given level, or 0 if the mob's level is below every band. */
    private double chanceForLevel(ConfigurationSection affixSection, String bandKey, int level) {
        ConfigurationSection bands = affixSection.getConfigurationSection(bandKey);
        if (bands == null) return 0.0;

        double chance = 0.0;
        int bestThreshold = -1;
        for (String key : bands.getKeys(false)) {
            try {
                int threshold = Integer.parseInt(key);
                if (level >= threshold && threshold > bestThreshold) {
                    bestThreshold = threshold;
                    chance = bands.getDouble(key, 0.0);
                }
            } catch (NumberFormatException ignored) {
                // non-numeric band key — skip
            }
        }
        return chance;
    }

    private void applySelf(LivingEntity entity, MobAffix affix, int level) {
        switch (affix) {
            case SWIFT -> applySwift(entity, level);
            case WRAITH -> entity.setInvisible(true);
            case REGENERATING -> applyRegenerating(entity, level);
            case BLEEDING, POISONOUS, CHILLING -> { /* on-hit — handled by MobAffixCombatListener */ }
        }
    }

    private void applySwift(LivingEntity entity, int level) {
        AttributeInstance speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed == null) return;
        ConfigurationSection roster = plugin.getConfig().getConfigurationSection("mob_scaling.affixes.roster.SWIFT");
        int pct = level >= 25
                ? (roster != null ? roster.getInt("speed-bonus-pct-elite", 40) : 40)
                : (roster != null ? roster.getInt("speed-bonus-pct", 25) : 25);
        speed.addModifier(new AttributeModifier(swiftSpeedKey, pct / 100.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    private void applyRegenerating(LivingEntity entity, int level) {
        int amplifier = level >= 25 ? 1 : 0;
        entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, amplifier, true, false));

        AttributeInstance toughness = entity.getAttribute(Attribute.ARMOR_TOUGHNESS);
        if (toughness != null) {
            ConfigurationSection roster = plugin.getConfig().getConfigurationSection("mob_scaling.affixes.roster.REGENERATING");
            double bonus = roster != null ? roster.getDouble("armor-toughness-bonus", 2.0) : 2.0;
            toughness.addModifier(new AttributeModifier(regenToughnessKey, bonus, AttributeModifier.Operation.ADD_NUMBER));
        }
    }
}
