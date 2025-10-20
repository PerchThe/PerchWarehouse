package dev.ev1dent.perchWarehouse;

import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record IUser(Player base) {

    private static WarehousePlugin plugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    static MessageManager message = new MessageManager(plugin());

    public static IUser getUser(Player player) {
        return new IUser(player);
    }

    public Player getBase() {
        return base;
    }

    public void teleport(Location location) {
        base.teleport(location);
    }
}
