package dev.ev1dent.perchWarehouse.data;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One warehouse (arena), loaded from warehouses/<name>.yml.
 */
public class Warehouse {

    /**
     * One crate rarity that can spawn in this warehouse.
     *
     * @param rarity  name of the loot/<rarity>.yml file (lowercase)
     * @param weight  how often this rarity is picked compared to the others
     * @param block   the block placed in the world
     * @param minimum always spawn at least this many (if there are enough spots)
     * @param maximum never spawn more than this many, -1 for no limit
     */
    public record CrateType(String rarity, int weight, Material block, int minimum, int maximum) {
        public boolean hasMaximum() { return maximum >= 0; }
    }

    private final String name;
    private final File file;
    private int count;
    private String worldName;
    private int randomSpawns;
    private final List<CrateType> crateTypes = new ArrayList<>();
    private final Map<MarkerType, List<SerializableLocation>> markers = new EnumMap<>(MarkerType.class);

    public Warehouse(String name, File file, int count, String worldName, int randomSpawns) {
        this.name = name;
        this.file = file;
        this.count = count;
        this.worldName = worldName;
        this.randomSpawns = randomSpawns;
        for (MarkerType type : MarkerType.values()) markers.put(type, new ArrayList<>());
    }

    public String getName() { return name; }
    public File getFile() { return file; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public World getWorld() { return worldName == null ? null : Bukkit.getWorld(worldName); }

    /** how many of the random crate spots get a crate each event */
    public int getRandomSpawns() { return randomSpawns; }

    public List<CrateType> getCrateTypes() { return crateTypes; }

    public List<SerializableLocation> getMarkers(MarkerType type) {
        return markers.get(type);
    }

    public int getTotalMarkers() {
        int total = 0;
        for (List<SerializableLocation> list : markers.values()) total += list.size();
        return total;
    }

    /** Which marker type (if any) sits on this block. */
    public MarkerType getMarkerAt(int x, int y, int z) {
        for (MarkerType type : MarkerType.values()) {
            for (SerializableLocation loc : markers.get(type)) {
                if (loc.blockX() == x && loc.blockY() == y && loc.blockZ() == z) return type;
            }
        }
        return null;
    }

    /**
     * Returns null when the warehouse can host an event, otherwise a short reason why it can't.
     */
    public String getProblem() {
        if (getWorld() == null) return "world '" + worldName + "' is not loaded";
        if (markers.get(MarkerType.PLAYER_SPAWN).isEmpty()) return "no player spawns";
        if (markers.get(MarkerType.GUARANTEED).isEmpty() && markers.get(MarkerType.RANDOM).isEmpty()) return "no crate spots";
        if (crateTypes.isEmpty()) return "no crates set up";
        return null;
    }

    public boolean isReady() {
        return getProblem() == null;
    }
}
