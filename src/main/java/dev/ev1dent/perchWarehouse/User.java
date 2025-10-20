package dev.ev1dent.perchWarehouse;

import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import dev.ev1dent.perchWarehouse.utilities.MiniUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record User(Player base) {

    private static WarehousePlugin plugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    static MessageManager message = new MessageManager(plugin());

    public static User getUser(Player player) {
        return new User(player);
    }

    public Player getBase() {
        return base;
    }

    public void teleport(Location location) {
        base.teleport(location);
    }

    public void sendMessage(String key) {
        base.sendMessage(MiniUtil.format(message.getMessage(key)));
    }
}
