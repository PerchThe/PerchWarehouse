package dev.ev1dent.perchWarehouse.exceptions;

import org.bukkit.entity.Player;

public class QueueClosedException extends Exception {

    public QueueClosedException(Player player, String exception){
        player.sendMessage(exception);
    }

}
