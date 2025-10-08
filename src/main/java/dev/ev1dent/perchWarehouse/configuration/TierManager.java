package dev.ev1dent.perchWarehouse.configuration;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class TierManager {

    private final WarehousePlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public TierManager(WarehousePlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "warehouses.yml");
        load();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!configFile.exists()) {
             plugin.saveResource("warehouses.yml", false);
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create warehouses.yml: " + e.getMessage());
            }
        }
        reload();
    }

    public String getTierForPlayerCount(int playerCount) {
        if (config == null) reload();

        Map<String, Integer> tiers = new TreeMap<>();
        for (String key : config.getKeys(false)) {
            if (config.isConfigurationSection(key)) {
                int count = config.getInt(key + ".count", -1);
                if (count >= 0) {
                    tiers.put(key, count);
                }
            }
        }
        if (tiers.isEmpty()) return null;

        String best = null;
        int bestCount = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : tiers.entrySet()) {
            int c = e.getValue();
            if (c >= playerCount && c < bestCount) {
                best = e.getKey();
                bestCount = c;
            }
        }

        if (best != null) return best;

        return tiers.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}
