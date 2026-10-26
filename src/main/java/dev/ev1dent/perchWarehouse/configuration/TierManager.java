package dev.ev1dent.perchWarehouse.configuration;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.data.MarkerType;
import dev.ev1dent.perchWarehouse.data.SerializableLocation;
import dev.ev1dent.perchWarehouse.data.Warehouse;
import org.bukkit.Material;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads, validates and saves plugins/PerchWarehouse/warehouses/<name>.yml. One file per warehouse (tier),
 * the file name is the warehouse name.
 */
public class TierManager {

    private static final String[] DEFAULT_FILES = {"small_warehouse.yml", "medium_warehouse.yml", "large_warehouse.yml"};

    private final WarehousePlugin plugin;
    private final File folder;
    private final Map<String, Warehouse> warehouses = new LinkedHashMap<>();

    public TierManager(WarehousePlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "warehouses");
        // only on first install, so deleting a default warehouse doesn't bring it back
        if (!folder.exists()) {
            folder.mkdirs();
            for (String name : DEFAULT_FILES) plugin.saveResource("warehouses/" + name, false);
        }
        reload();
    }

    public void reload() {
        warehouses.clear();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;
        java.util.Arrays.sort(files);
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4);
            Warehouse warehouse = parse(name, file, YamlConfiguration.loadConfiguration(file));
            if (warehouse != null) warehouses.put(name, warehouse);
        }
    }

    private Warehouse parse(String name, File file, ConfigurationSection config) {
        int capacity = config.getInt("capacity", config.getInt("count", -1));
        if (capacity < 1) {
            warn(name, "capacity", "is missing or below 1, skipping this warehouse");
            return null;
        }
        String world = config.getString("world");
        if (world == null) warn(name, "world", "is missing, set it or place a marker with the editor");

        int randomSpawns = Math.max(0, config.getInt("crates.random-spawns", 0));
        Warehouse warehouse = new Warehouse(name, file, capacity, world, randomSpawns);

        ConfigurationSection rarities = config.getConfigurationSection("crates.rarities");
        if (rarities == null || rarities.getKeys(false).isEmpty()) {
            warn(name, "crates.rarities", "has no crates, this warehouse can't run until you add some");
        } else {
            for (String rarity : rarities.getKeys(false)) {
                Warehouse.CrateType type = parseCrate(name, "crates.rarities." + rarity, rarity, rarities.getConfigurationSection(rarity));
                if (type != null) warehouse.getCrateTypes().add(type);
            }
        }

        for (MarkerType type : MarkerType.values()) {
            String path = "markers." + type.getListKey();
            List<String> raw = config.getStringList(path);
            for (int i = 0; i < raw.size(); i++) {
                try {
                    warehouse.getMarkers(type).add(SerializableLocation.parse(raw.get(i)));
                } catch (IllegalArgumentException e) {
                    warn(name, path + "[" + i + "]", e.getMessage() + ", skipping");
                }
            }
        }
        return warehouse;
    }

    private Warehouse.CrateType parseCrate(String warehouse, String path, String rarity, ConfigurationSection section) {
        if (section == null) {
            warn(warehouse, path, "is not a section, skipping");
            return null;
        }
        int weight = section.getInt("weight", 1);
        if (weight < 0) {
            warn(warehouse, path + ".weight", "can't be negative, skipping this crate");
            return null;
        }
        Material block = Material.matchMaterial(String.valueOf(section.getString("block")));
        if (block == null || !block.isBlock() || block.isAir()) {
            warn(warehouse, path + ".block", "'" + section.getString("block") + "' is not a block, using SHULKER_BOX");
            block = Material.SHULKER_BOX;
        } else if (!isTileState(block)) {
            plugin.getLogger().info("warehouses/" + warehouse + ".yml -> " + path + ".block: " + block
                + " can't hold data, so leftover crates can't be found after a crash. A shulker box or chest is recommended.");
        }
        int minimum = Math.max(0, section.getInt("minimum-crate-spawns", 0));
        int maximum = section.getInt("maximum-crate-spawns", -1);
        if (maximum >= 0 && maximum < minimum) {
            warn(warehouse, path + ".maximum-crate-spawns", "is lower than minimum-crate-spawns, using " + minimum);
            maximum = minimum;
        }
        if (weight == 0 && minimum == 0)
            warn(warehouse, path, "has weight 0 and no minimum, it will never spawn");
        return new Warehouse.CrateType(rarity.toLowerCase(Locale.ROOT), weight, block, minimum, maximum);
    }

    private boolean isTileState(Material material) {
        try {
            return material.createBlockData().createBlockState() instanceof TileState;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Checks every crate points at a real loot/<rarity>.yml, after both folders are loaded. */
    public void validateRarities(LootManager lootManager) {
        for (Warehouse warehouse : warehouses.values()) {
            int guaranteedMinimums = 0;
            for (Warehouse.CrateType type : warehouse.getCrateTypes()) {
                if (lootManager.getRarity(type.rarity()) == null)
                    warn(warehouse.getName(), "crates.rarities." + type.rarity(), "there is no loot/" + type.rarity() + ".yml, these crates won't spawn");
                guaranteedMinimums += type.minimum();
            }
            int spots = warehouse.getMarkers(MarkerType.GUARANTEED).size()
                + Math.min(warehouse.getRandomSpawns(), warehouse.getMarkers(MarkerType.RANDOM).size());
            if (spots > 0 && guaranteedMinimums > spots)
                warn(warehouse.getName(), "crates.rarities", "the minimums add up to " + guaranteedMinimums
                    + " but only " + spots + " crates spawn per event, some minimums can't be met");
        }
    }

    public Collection<Warehouse> getWarehouses() {
        return warehouses.values();
    }

    public Warehouse getWarehouse(String name) {
        return warehouses.get(name);
    }

    public boolean exists(String name) {
        return warehouses.containsKey(name);
    }

    /** Writes a new warehouses/<name>.yml with a starter crate setup. */
    public Warehouse create(String name, int capacity, String worldName) {
        File file = new File(folder, name + ".yml");
        if (warehouses.containsKey(name) || file.exists()) return null;
        YamlConfiguration config = new YamlConfiguration();
        config.set("capacity", capacity);
        config.set("world", worldName);
        config.set("crates.random-spawns", Math.max(1, capacity / 3));
        config.set("crates.rarities.common.weight", 100);
        config.set("crates.rarities.common.block", Material.GREEN_SHULKER_BOX.name().toLowerCase(Locale.ROOT));
        config.set("crates.rarities.uncommon.weight", 50);
        config.set("crates.rarities.uncommon.block", Material.BLUE_SHULKER_BOX.name().toLowerCase(Locale.ROOT));
        config.set("crates.rarities.rare.weight", 10);
        config.set("crates.rarities.rare.block", Material.RED_SHULKER_BOX.name().toLowerCase(Locale.ROOT));
        config.set("crates.rarities.rare.minimum-crate-spawns", 1);
        for (MarkerType type : MarkerType.values()) config.set("markers." + type.getListKey(), new ArrayList<String>());
        if (!saveFile(config, file)) return null;
        Warehouse warehouse = parse(name, file, config);
        if (warehouse != null) warehouses.put(name, warehouse);
        return warehouse;
    }

    /** Removes the warehouse and its file. */
    public boolean delete(String name) {
        Warehouse warehouse = warehouses.remove(name);
        if (warehouse == null) return false;
        if (!warehouse.getFile().delete()) plugin.getLogger().warning("Could not delete " + warehouse.getFile().getName());
        return true;
    }

    /** Writes a warehouse's world and markers back to its file (used by the editor). Leaves everything else as typed. */
    public void save(Warehouse warehouse) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(warehouse.getFile());
        config.set("world", warehouse.getWorldName());
        for (MarkerType type : MarkerType.values()) {
            List<String> raw = new ArrayList<>();
            for (SerializableLocation loc : warehouse.getMarkers(type)) {
                raw.add(type == MarkerType.PLAYER_SPAWN ? loc.serializeSpawn() : loc.serializeBlock());
            }
            config.set("markers." + type.getListKey(), raw);
        }
        saveFile(config, warehouse.getFile());
    }

    private boolean saveFile(YamlConfiguration config, File file) {
        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warehouses/" + file.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Picks the smallest ready warehouse that fits the player count. If more players queued than
     * any warehouse is built for, the biggest one is used and players share spawns.
     */
    public Warehouse getTierForPlayerCount(int playerCount) {
        Warehouse best = null;
        Warehouse biggest = null;
        for (Warehouse warehouse : warehouses.values()) {
            if (!warehouse.isReady()) continue;
            int c = warehouse.getCount();
            if (c >= playerCount && (best == null || c < best.getCount())) best = warehouse;
            if (biggest == null || c > biggest.getCount()) biggest = warehouse;
        }
        return best != null ? best : biggest;
    }

    private void warn(String warehouse, String path, String problem) {
        plugin.getLogger().warning("warehouses/" + warehouse + ".yml -> " + path + ": " + problem);
    }
}
