package dev.ev1dent.perchWarehouse.listeners;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // the server went down while they were in the editor, give their inventory back
        if (warehousePlugin().getEditorManager().restoreBackup(e.getPlayer())) {
            Utils.send(e.getPlayer(), "editor-restored-after-crash");
        }
        warehousePlugin().getWarehouseManager().handleJoin(e.getPlayer());
    }
}
