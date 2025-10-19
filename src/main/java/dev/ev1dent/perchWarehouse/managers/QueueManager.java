package dev.ev1dent.perchWarehouse.managers;

import dev.ev1dent.perchWarehouse.exceptions.QueueClosedException;
import dev.ev1dent.perchWarehouse.utilities.LoggerUtil;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class QueueManager {

    private final Queue<UUID> playerQueue;
    public boolean isOpened;

    public QueueManager() {
        this.playerQueue = new LinkedList<>();
    }

    public void addPlayer(Player player) throws QueueClosedException {
        if(!isOpened){
            throw new QueueClosedException(player);
        }
        LoggerUtil.debug("adding " + player.getName() + " to the queue");
        UUID playerUUID = player.getUniqueId();
        if(isQueued(player)) return;
        playerQueue.add(playerUUID);
    }

    public void removePlayer(Player player) throws QueueClosedException {
        if(!isOpened){
            throw new QueueClosedException(player);
        }
        LoggerUtil.debug("removing " + player.getName() + " from the queue");
        UUID playerUUID = player.getUniqueId();
        if(!isQueued(player)) return;
        playerQueue.remove(playerUUID);
    }

    public boolean isQueued(Player player) {
        return playerQueue.contains(player.getUniqueId());
    }

    public int getQueueSize() {
        return playerQueue.size();
    }

    public void clearQueue() {
        playerQueue.clear();
    }


    public LinkedList<UUID> getPlayersInQueue() {
        return new LinkedList<>(playerQueue);
    }

}