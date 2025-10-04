package dev.ev1dent.perchWarehouse.configuration;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class MessageManager {

    private WarehousePlugin plugin;
    private File messageFile;
    private FileConfiguration config;

    public MessageManager(WarehousePlugin plugin) {
        this.plugin = plugin;
        this.messageFile = new File(plugin.getDataFolder(), "messages.yml");
        loadConfig();
    }

    public void reload() {
        if (!messageFile.exists()) {
            plugin.saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(messageFile);
    }

    public void save() {
        try {
            config.save(messageFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save message file", e);
        }
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            reload();
        }
        return config;
    }

    private void loadConfig() {
        if (!messageFile.exists()) {
            messageFile.getParentFile().mkdirs();
            plugin.saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(messageFile);
    }

    public String getMessage(String key) {
        return getConfig().getString(key);
    }

}
