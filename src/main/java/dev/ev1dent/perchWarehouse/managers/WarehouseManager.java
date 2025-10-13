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
        TaskChain.newChain().delay(time).add(new TaskChain.GenericTask() {

            @Override
            protected void run() {
                queueManager.isOpened = false;
                start();
            }
        }).execute();
    }

    public void start(){
        LoggerUtil.debug("WarehouseManager.start()");
        teleportPlayers();

    }

    public void stop(){
        LoggerUtil.debug("WarehouseManager.stop()");
    }

    private void teleportPlayers(){
        queueManager.getPlayersInQueue().forEach(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);
            Location location = new Location(player.getWorld(), player.getLocation().getX() + 100, player.getLocation().getY() + 100, player.getLocation().getZ() + 100);
            player.teleport(location);

        });
    }
}
