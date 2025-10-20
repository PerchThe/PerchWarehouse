package dev.ev1dent.perchWarehouse;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record IUser(Player base) {

    private static WarehousePlugin plugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    public static IUser getUser(Player player) {
        return new IUser(player);
    }

    public void teleport(Location location) {
        User user = Essentials.getPlugin(Essentials.class).getUser(base.getUniqueId());
        user.setLastLocation(base.getLocation());
        base.teleport(location);
    }

    public void sendBack(){
        User user = Essentials.getPlugin(Essentials.class).getUser(base.getUniqueId());
        base.teleport(user.getLastLocation());
    }
}
