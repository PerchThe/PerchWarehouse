package dev.ev1dent.perchWarehouse.exceptions;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import dev.ev1dent.perchWarehouse.utilities.MiniUtil;
import org.bukkit.entity.Player;

public class QueueClosedException extends Exception {
    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }
    MessageManager messageManager = new MessageManager(warehousePlugin());

    public QueueClosedException(Player player){
        player.sendMessage(MiniUtil.format(messageManager.getMessage("queue-closed")));
    }

}
