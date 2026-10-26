package dev.ev1dent.perchWarehouse.listeners;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {
    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // boxes left behind by a crash still carry our tag, remove them as their chunks load
        if (!event.isNewChunk()) warehousePlugin().getWarehouseManager().checkChunkForOrphans(event.getChunk());
        warehousePlugin().getEditorManager().handleChunkLoad(event.getChunk());
    }
}
