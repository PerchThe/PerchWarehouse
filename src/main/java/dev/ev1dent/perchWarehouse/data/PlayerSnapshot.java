package dev.ev1dent.perchWarehouse.data;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

/**
 * What we need to put a player back the way they were before the event.
 */
public record PlayerSnapshot(UUID uuid, String world, SerializableLocation location, GameMode gameMode) {

    public static PlayerSnapshot of(org.bukkit.entity.Player player) {
        Location loc = player.getLocation();
        return new PlayerSnapshot(player.getUniqueId(), loc.getWorld().getName(),
            new SerializableLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()),
            player.getGameMode());
    }

    /** null if the world is gone */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : location.toLocation(w);
    }

    public void save(ConfigurationSection section) {
        section.set("world", world);
        section.set("location", location.serializeSpawn());
        section.set("gamemode", gameMode.name());
    }

    public static PlayerSnapshot load(UUID uuid, ConfigurationSection section) {
        GameMode mode;
        try {
            mode = GameMode.valueOf(section.getString("gamemode", "SURVIVAL"));
        } catch (IllegalArgumentException e) {
            mode = GameMode.SURVIVAL;
        }
        return new PlayerSnapshot(uuid, section.getString("world"),
            SerializableLocation.parse(section.getString("location")), mode);
    }
}
