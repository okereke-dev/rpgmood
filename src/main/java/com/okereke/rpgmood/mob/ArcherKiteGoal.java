package com.okereke.rpgmood.mob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.EnumSet;

/**
 * Level 30+ archer kiting: when the target player is close with a melee weapon out,
 * back away while keeping the player as target.
 */
public final class ArcherKiteGoal implements Goal<Mob> {

    private final GoalKey<Mob> key;
    private final Mob mob;
    private final double keepDistance;
    private final double keepDistanceSq;

    public ArcherKiteGoal(Plugin plugin, Mob mob, double keepDistance) {
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "archer_kite"));
        this.mob = mob;
        this.keepDistance = Math.max(4.0, keepDistance);
        this.keepDistanceSq = this.keepDistance * this.keepDistance;
    }

    @Override
    public boolean shouldActivate() {
        if (!(mob.getTarget() instanceof Player player)) return false;
        if (!player.isValid() || player.isDead()) return false;
        if (mob.getLocation().distanceSquared(player.getLocation()) > keepDistanceSq) return false;
        return isMeleeThreat(player);
    }

    @Override
    public boolean shouldStayActive() {
        return shouldActivate();
    }

    @Override
    public void tick() {
        if (!(mob.getTarget() instanceof Player player)) return;
        Vector away = mob.getLocation().toVector().subtract(player.getLocation().toVector());
        if (away.lengthSquared() < 1.0E-4) {
            away = player.getLocation().getDirection().multiply(-1);
        }
        away.normalize();
        var dest = mob.getLocation().add(away.multiply(keepDistance));
        mob.getPathfinder().moveTo(dest, 1.15);
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }

    private static boolean isMeleeThreat(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main == null || main.getType().isAir()) return true;
        String name = main.getType().name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("TRIDENT") || name.equals("MACE");
    }
}
