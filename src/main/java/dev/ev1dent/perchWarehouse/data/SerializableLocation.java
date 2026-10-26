package dev.ev1dent.perchWarehouse.data;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Locale;

/**
 * A world-less location stored as a string in the yml files.
 * Player spawns use "x,y,z,yaw,pitch", box markers use block coordinates "x,y,z".
 */
public record SerializableLocation(double x, double y, double z, float yaw, float pitch) {

    public static SerializableLocation parse(String input) {
        if (input == null) throw new IllegalArgumentException("missing location");
        String[] split = input.split(",");
        if (split.length != 3 && split.length != 5)
            throw new IllegalArgumentException("'" + input + "' should be x,y,z or x,y,z,yaw,pitch");
        try {
            double x = Double.parseDouble(split[0].trim());
            double y = Double.parseDouble(split[1].trim());
            double z = Double.parseDouble(split[2].trim());
            float yaw = split.length == 5 ? Float.parseFloat(split[3].trim()) : 0f;
            float pitch = split.length == 5 ? Float.parseFloat(split[4].trim()) : 0f;
            return new SerializableLocation(x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + input + "' has a value that isn't a number");
        }
    }

    public static SerializableLocation ofBlock(Block block) {
        return new SerializableLocation(block.getX(), block.getY(), block.getZ(), 0f, 0f);
    }

    /** Centers the spawn on the block and keeps the direction the admin was facing (snapped to 45 degrees). */
    public static SerializableLocation ofSpawn(Block block, float yaw) {
        float snapped = Math.round(yaw / 45f) * 45f;
        return new SerializableLocation(block.getX() + 0.5, block.getY(), block.getZ() + 0.5, snapped, 0f);
    }

    public int blockX() { return (int) Math.floor(x); }
    public int blockY() { return (int) Math.floor(y); }
    public int blockZ() { return (int) Math.floor(z); }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Block toBlock(World world) {
        return world.getBlockAt(blockX(), blockY(), blockZ());
    }

    public boolean sameBlock(SerializableLocation other) {
        return blockX() == other.blockX() && blockY() == other.blockY() && blockZ() == other.blockZ();
    }

    public boolean sameBlock(Block block) {
        return blockX() == block.getX() && blockY() == block.getY() && blockZ() == block.getZ();
    }

    public String serializeBlock() {
        return blockX() + "," + blockY() + "," + blockZ();
    }

    public String serializeSpawn() {
        return trim(x) + "," + trim(y) + "," + trim(z) + "," + trim(yaw) + "," + trim(pitch);
    }

    private static String trim(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.format(Locale.ROOT, "%.2f", d);
    }
}
