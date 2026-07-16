package com.okereke.rpgmood.mob;

import com.okereke.rpgmood.RPGMoodPlugin;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;

/**
 * Companion to {@link com.okereke.rpgmood.MobAuraEffect}: draws a second, affix-colored
 * particle marker above an affixed mob's level-color aura, and sends a one-time action-bar
 * warning to the first player who gets close enough to notice. Runs on the same 20-tick
 * cadence and 30-block proximity filter MobAuraEffect already uses, no extra world scan.
 */
public class MobAffixAuraTask extends BukkitRunnable {

    private static final double AURA_PROXIMITY_RADIUS_SQ = 900.0; // 30 blocks, matches MobAuraEffect

    /** Tripped on the first spawnParticle failure — mirrors MobAuraEffect's guard, some server/Paper builds reject the DustOptions data type this uses. */
    private static volatile boolean particlesDisabled = false;

    private final RPGMoodPlugin plugin;

    public MobAffixAuraTask(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        NamespacedKey affixesKey = plugin.getMobAffixService().getAffixesKey();
        plugin.getServer().getWorlds().forEach(world ->
                world.getLivingEntities().stream()
                        .filter(entity -> entity.getPersistentDataContainer().has(affixesKey, PersistentDataType.STRING))
                        .forEach(this::handle)
        );
    }

    private void handle(LivingEntity entity) {
        if (entity.isDead() || !entity.isValid()) return;

        String tag = entity.getPersistentDataContainer().get(plugin.getMobAffixService().getAffixesKey(), PersistentDataType.STRING);
        if (tag == null || tag.isBlank()) return;

        double warningRadiusSq = warningRadiusSquared();
        boolean nearAnyPlayer = false;
        boolean alreadyWarned = entity.getScoreboardTags().contains("rpgmood_affix_warned");

        for (Player player : entity.getWorld().getPlayers()) {
            double distanceSq = player.getLocation().distanceSquared(entity.getLocation());
            if (distanceSq <= AURA_PROXIMITY_RADIUS_SQ) {
                nearAnyPlayer = true;
            }
            if (!alreadyWarned && distanceSq <= warningRadiusSq) {
                warn(player, entity, tag);
                entity.addScoreboardTag("rpgmood_affix_warned");
                alreadyWarned = true;
            }
        }

        if (nearAnyPlayer && !particlesDisabled) {
            spawnMarker(entity);
        }
    }

    private double warningRadiusSquared() {
        int radius = plugin.getConfig().getInt("mob_scaling.affixes.warning-radius", 12);
        return (double) radius * radius;
    }

    private void spawnMarker(LivingEntity entity) {
        try {
            entity.getWorld().spawnParticle(Particle.DUST,
                    entity.getLocation().add(0, entity.getHeight() + 0.3, 0),
                    1, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 40, 220), 0.5f));
        } catch (IllegalArgumentException e) {
            particlesDisabled = true;
            plugin.getLogger().warning("Mob affix particle markers disabled — this server's Particle API rejected the expected data type (" + e.getMessage() + "). This is a server/Paper-version incompatibility, not a config issue; affix warnings still work, just without the marker.");
        }
    }

    private void warn(Player player, LivingEntity entity, String affixTag) {
        String[] names = affixTag.split(",");
        if (names.length == 0) return;
        String affixDisplay = MobAffix.valueOf(names[0]).getDisplayName();
        String mobName = prettify(entity.getType().name());
        plugin.getMessageService().send(player, "&e⚠ This " + mobName + " is " + affixDisplay + "!");
    }

    private String prettify(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
