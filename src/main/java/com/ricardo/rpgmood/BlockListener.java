package com.ricardo.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Random;

public class BlockListener implements Listener {

    private final RPGMoodPlugin plugin;
    private final Random random = new Random();

    public BlockListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        String blockName = event.getBlock().getType().name();
        var triggerSection = plugin.getConfigManager().getTriggers().getConfigurationSection("block_events");
        if (triggerSection == null) {
            return;
        }
        for (String key : triggerSection.getKeys(false)) {
            var section = triggerSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            if (blockName.equalsIgnoreCase(section.getString("block", "")) && random.nextInt(100) < section.getInt("chance", 0)) {
                Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(section.getString("message", ""));
                event.getPlayer().sendMessage(message);
            }
        }
    }
}
