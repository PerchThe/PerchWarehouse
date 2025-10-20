package dev.ev1dent.perchWarehouse.managers;

import dev.ev1dent.perchWarehouse.Events.QueueOpenEvent;
import dev.ev1dent.perchWarehouse.IUser;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.TierManager;
import dev.ev1dent.perchWarehouse.utilities.TaskChain;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WarehouseManager {

    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }
    QueueManager queueManager = warehousePlugin().getQueueManager();
    TierManager tierManager = new TierManager(warehousePlugin());

    public void create(String name, int capacity) {
        Utils.getDebugLogger("WarehouseManager.create() " + name + " " + capacity);
    }

    public void edit(String name){
        Utils.getDebugLogger("WarehouseManager.edit() " + name);
    }

    public void delete(String name){
        Utils.getDebugLogger("WarehouseManager.delete() "+ name);
    }

    public void openRegistration(int time){
        QueueOpenEvent event = new QueueOpenEvent();
        Bukkit.getPluginManager().callEvent(event);

        queueManager.isOpened = true;
        Utils.getDebugLogger("Queue Opened for " + time + " minutes");
        TaskChain.newChain().delay(time * 1200).add(new TaskChain.GenericTask() {

            @Override
            protected void run() {
                if (queueManager.isOpened) {
                    start();
                }
                queueManager.isOpened = false;
                queueManager.clearQueue();
            }
        }).execute();
    }

    public void start(){
        Utils.getDebugLogger("Checking Warehouse Tier");
        String warehouseTier = tierManager.getTierForPlayerCount(queueManager.getQueueSize());
        Utils.getDebugLogger("Warehouse Tier is " + warehouseTier + queueManager.getQueueSize() + " players");
        //TODO get configuration for warehouseTier and continue
        teleportPlayers();
        Utils.getDebugLogger("Closing Queue");
        queueManager.isOpened = false;
    }

    public void stop(Player player){
        queueManager.isOpened = false;
        Utils.getDebugLogger("WarehouseManager.stop()");

        queueManager.clearQueue();
    }

    private void teleportPlayers(){
        queueManager.getPlayersInQueue().forEach(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);
            //TODO get random spawn location from warehouses.yml random spawn locations
            Location location = new Location(player.getWorld(), player.getLocation().getX() + 100, player.getLocation().getY() + 100, player.getLocation().getZ() + 100);
            IUser.getUser(player).teleport(location);

        });
    }
}
