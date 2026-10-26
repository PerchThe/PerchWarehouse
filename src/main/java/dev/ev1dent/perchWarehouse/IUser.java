package dev.ev1dent.perchWarehouse;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public record IUser(Player base) {

    private static WarehousePlugin plugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    public static IUser getUser(Player player) {
        return new IUser(player);
    }

    private User essentialsUser() {
        Essentials essentials = Essentials.getPlugin(Essentials.class);
        return essentials.getUser(base.getUniqueId());
    }

    /** Saves /back so players can get back to where they were, then teleports. */
    public void teleport(Location location) {
        setBackLocation();
        base.teleport(location);
    }

    /**
     * Async teleport (loads the chunk first), also saving /back.
     * The future completes when the player is actually there.
     */
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        setBackLocation();
        return base.teleportAsync(location);
    }

    public void setBackLocation() {
        User user = essentialsUser();
        if (user != null) user.setLastLocation(base.getLocation());
    }

    public void sendBack(){
        User user = essentialsUser();
        if (user != null && user.getLastLocation() != null) base.teleport(user.getLastLocation());
    }
}
