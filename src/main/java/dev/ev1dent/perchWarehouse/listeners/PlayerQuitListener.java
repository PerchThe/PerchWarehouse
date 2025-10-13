package dev.ev1dent.perchWarehouse.listeners;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.exceptions.QueueClosedException;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }
    QueueManager queueManager = warehousePlugin().getQueueManager();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) throws QueueClosedException {
        if(queueManager.isQueued(e.getPlayer())){
            queueManager.removePlayer(e.getPlayer());
        }
    }
}
