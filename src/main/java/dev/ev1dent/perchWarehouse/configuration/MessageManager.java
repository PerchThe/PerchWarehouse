package dev.ev1dent.perchWarehouse.configuration;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MessageManager {

    private final WarehousePlugin plugin;
    private FileConfiguration dataConfig = null;
    private File configFile = null;

    public MessageManager(WarehousePlugin plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
    }

    public void reloadConfig() {
        if (this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), "messages.yml");

        this.dataConfig = YamlConfiguration.loadConfiguration(this.configFile);

        // missing keys fall back to the messages.yml inside the jar, so new messages work after updating
        InputStream defaultStream = this.plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
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
            this.configFile = new File(this.plugin.getDataFolder(), "messages.yml");

        if (!this.configFile.exists()) {
            this.plugin.saveResource("messages.yml", false);
        }
    }

    public String getMessage(String key) {
        return getConfig().getString(key);
    }
}
