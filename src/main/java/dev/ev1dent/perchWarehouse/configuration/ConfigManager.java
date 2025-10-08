package dev.ev1dent.perchWarehouse.configuration;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigManager {

    private final WarehousePlugin plugin;
    private FileConfiguration dataConfig = null;
    private File configFile = null;

    public ConfigManager(WarehousePlugin plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
    }

    public void reloadConfig() {
        if (this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), "config.yml");

        this.dataConfig = YamlConfiguration.loadConfiguration(this.configFile);

        InputStream defaultStream = this.plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.dataConfig.setDefaults(defaultConfig);
        }
    }

    public FileConfiguration getConfig() {
        if (this.dataConfig == null)
            reloadConfig();

        return this.dataConfig;
    }

    public void saveDefaultConfig() {
        if (this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), "config.yml");

        if (!this.configFile.exists()) {
            this.plugin.saveResource("config.yml", false);
        }
    }

    public String getString(String key) {
        return getConfig().getString(key);
    }

    public boolean getBoolean(String value){
        return getConfig().getBoolean(value);
    }
}