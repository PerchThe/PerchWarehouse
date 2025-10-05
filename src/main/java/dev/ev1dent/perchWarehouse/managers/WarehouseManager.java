package dev.ev1dent.perchWarehouse.managers;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WarehouseManager {

    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }
    QueueManager queueManager = warehousePlugin().getQueueManager();

    public void create(String name, int capacity) {
        System.out.println("WarehouseManager.create() " + name + " " + capacity);
    }

    public void edit(String name){
        System.out.println("WarehouseManager.edit() " + name);
    }

    public void delete(String name){
        System.out.println("WarehouseManager.delete() "+ name);
    }

    public void start(){
        System.out.println("WarehouseManager.start()");
        teleportPlayers();

    }

    public void stop(){
        System.out.println("WarehouseManager.stop()");
    }

    private void teleportPlayers(){
        queueManager.getPlayersInQueue().forEach(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);
            Location location = new Location(player.getWorld(), player.getLocation().getX() + 100, player.getLocation().getY() + 100, player.getLocation().getZ() + 100);
            player.teleport(location);

        });
    }
}
