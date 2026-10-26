package dev.ev1dent.perchWarehouse.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * A block position that is safe to use as a map key (Location uses doubles and holds a world reference).
 */
public record BlockKey(String world, int x, int y, int z) {

    public static BlockKey of(Block block) {
        return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockKey of(Location location) {
        return new BlockKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public World getWorld() {
        return Bukkit.getWorld(world);
    }

    /** null if the world isn't loaded */
    public Block toBlock() {
        World w = getWorld();
        return w == null ? null : w.getBlockAt(x, y, z);
    }

    public Location center() {
        return new Location(getWorld(), x + 0.5, y + 0.5, z + 0.5);
    }

    public String serialize() {
        return world + "," + x + "," + y + "," + z;
    }

    public static BlockKey parse(String raw) {
        String[] split = raw.split(",");
        if (split.length != 4) throw new IllegalArgumentException("'" + raw + "' should be world,x,y,z");
        try {
            return new BlockKey(split[0], Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + raw + "' has a value that isn't a number");
        }
    }
}
