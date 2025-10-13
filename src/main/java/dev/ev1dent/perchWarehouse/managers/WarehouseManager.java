package dev.ev1dent.perchWarehouse.managers;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.utilities.LoggerUtil;
import dev.ev1dent.perchWarehouse.utilities.TaskChain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WarehouseManager {

    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }
    QueueManager queueManager = warehousePlugin().getQueueManager();

    public void create(String name, int capacity) {
        LoggerUtil.debug("WarehouseManager.create() " + name + " " + capacity);
    }

    public void edit(String name){
        LoggerUtil.debug("WarehouseManager.edit() " + name);
    }

    public void delete(String name){
        LoggerUtil.debug("WarehouseManager.delete() "+ name);
    }

    public void openRegistration(int time){
        queueManager.isOpened = true;
        LoggerUtil.debug("Queue Opened for " + time + " minutes");
        TaskChain.newChain().delay(time * 1200).add(new TaskChain.GenericTask() {

            @Override
            protected void run() {
                queueManager.isOpened = false;
                start();
            }
        }).execute();
    }

    public void start(){
        LoggerUtil.debug("Starting warehouse.");
        teleportPlayers();
        LoggerUtil.debug("Closing Queue");
        queueManager.isOpened = false;
    }

    public void stop(){
        LoggerUtil.debug("WarehouseManager.stop()");
        //TODO teleport all players to their back location
        queueManager.clearQueue();
    }

    private void updateBackLocation(Player player){
        LoggerUtil.debug("Updating lastLocation for " + player.getName());
        //TODO Hook into Essentials and set lastLocation. see PerchArtmap for how-to
    }

    private void teleportPlayers(){
        queueManager.getPlayersInQueue().forEach(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);
            updateBackLocation(player);
            //TODO get random spawn location from warehouses.yml random spawn locations
            Location location = new Location(player.getWorld(), player.getLocation().getX() + 100, player.getLocation().getY() + 100, player.getLocation().getZ() + 100);
            player.teleport(location);

        });
    }
}
