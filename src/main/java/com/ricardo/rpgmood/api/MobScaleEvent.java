package com.ricardo.rpgmood.api;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a mob's level is applied. Cancelling prevents scaling.
 * The final level can be modified via {@link #setLevel(int)}.
 */
public class MobScaleEvent extends EntityEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private int level;
    private boolean cancelled;

    public MobScaleEvent(@NotNull LivingEntity entity, int level) {
        super(entity);
        this.level = level;
    }

    @Override
    public @NotNull LivingEntity getEntity() { return (LivingEntity) entity; }

    /** The level that will be applied. Modify via {@link #setLevel(int)}. */
    public int getLevel() { return level; }

    /** Override the calculated level before scaling is applied. */
    public void setLevel(int level) { this.level = Math.max(1, level); }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
