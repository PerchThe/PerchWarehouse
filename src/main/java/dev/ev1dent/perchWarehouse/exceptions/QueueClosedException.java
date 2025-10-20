package dev.ev1dent.perchWarehouse.exceptions;

import dev.ev1dent.perchWarehouse.utilities.Utils;
import org.bukkit.entity.Player;

public class QueueClosedException extends Exception {

    public QueueClosedException(Player player){
        player.sendMessage(Utils.format("queue-closed"));
    }

}
