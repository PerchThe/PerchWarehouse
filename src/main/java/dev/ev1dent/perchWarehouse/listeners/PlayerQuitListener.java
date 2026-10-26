package dev.ev1dent.perchWarehouse.listeners;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }
    QueueManager queueManager = warehousePlugin().getQueueManager();

    /**
     * Quitting does NOT remove you from the queue, it lives in queue.yml so you keep your spot when you relog.
     * Editors get their inventory back before their data is saved.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        warehousePlugin().getEditorManager().exit(e.getPlayer());
        warehousePlugin().getWarehouseManager().handleQuit(e.getPlayer());
    }

    /** Kicked (or banned) players lose their queue spot. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent e) {
        queueManager.removeUUID(e.getPlayer().getUniqueId());
    }
}
