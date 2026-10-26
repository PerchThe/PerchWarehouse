package dev.ev1dent.perchWarehouse.configuration;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class ConfigManager {

    public enum DeathMode { ELIMINATE, RESPAWN }

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
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            this.dataConfig.setDefaults(defaultConfig);
        }
        validate();
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

    public boolean getBoolean(String key){
        return getConfig().getBoolean(key);
    }

    public int getInt(String key){
        return getConfig().getInt(key);
    }

    public List<?> getList(String key){
        return getConfig().getList(key);
    }

    public List<String> getStringList(String key){
        return getConfig().getStringList(key);
    }

    public List<Integer> getIntegerList(String key){
        return getConfig().getIntegerList(key);
    }

    public ConfigurationSection getSection(String key){
        return getConfig().getConfigurationSection(key);
    }

    // ------------------------------------------------------------------ typed event settings

    public boolean isAutoStart(){ return getBoolean("event.auto-start"); }
    public long getIntervalMillis(){ return Math.max(1, getInt("event.interval-minutes")) * 60_000L; }
    public int getQueueOpenSeconds(){ return Math.max(5, getInt("event.queue-open-seconds")); }
    public List<Integer> getAnnounceAt(){ return getIntegerList("event.announce-at"); }
    public int getCountdownSeconds(){ return Math.max(0, getInt("event.countdown-seconds")); }
    public long getDurationMillis(){ return Math.max(1, getInt("event.duration-minutes")) * 60_000L; }
    public int getMinPlayers(){ return Math.max(1, getInt("event.min-players")); }
    public int getEndWhenLootedDelaySeconds(){ return Math.max(0, getInt("event.end-when-looted-delay-seconds")); }

    public GameMode getEventGameMode(){
        GameMode mode = parseEnum(GameMode.class, getString("event.gamemode"));
        return mode == null ? GameMode.ADVENTURE : mode;
    }

    public boolean isPvp(){ return getBoolean("combat.pvp"); }
    public boolean isKeepInventory(){ return getBoolean("combat.keep-inventory"); }

    public DeathMode getDeathMode(){
        DeathMode mode = parseEnum(DeathMode.class, getString("combat.on-death"));
        return mode == null ? DeathMode.ELIMINATE : mode;
    }

    public boolean isQueueBossBar(){ return getBoolean("display.queue-bossbar"); }
    public boolean isEventBossBar(){ return getBoolean("display.event-bossbar"); }
    public boolean isEditorCreative(){ return getBoolean("editor.creative"); }

    /**
     * Logs every bad value with its path instead of throwing, so one typo doesn't kill the plugin.
     * Bad values fall back to safe defaults in the getters above.
     */
    private void validate(){
        FileConfiguration c = this.dataConfig;
        if (parseEnum(GameMode.class, c.getString("event.gamemode")) == null)
            warn("event.gamemode", "'" + c.getString("event.gamemode") + "' is not a gamemode, using ADVENTURE");
        if (parseEnum(DeathMode.class, c.getString("combat.on-death")) == null)
            warn("combat.on-death", "must be ELIMINATE or RESPAWN, using ELIMINATE");
        for (String path : new String[]{"event.interval-minutes", "event.duration-minutes"}) {
            if (c.getInt(path) < 1) warn(path, "must be at least 1");
        }
        if (c.getInt("event.queue-open-seconds") < 5) warn("event.queue-open-seconds", "must be at least 5");
        if (c.getInt("event.min-players") < 1) warn("event.min-players", "must be at least 1");
        for (String marker : new String[]{"player-spawn", "guaranteed", "random"}) {
            String path = "editor.markers." + marker + ".material";
            Material material = Material.matchMaterial(String.valueOf(c.getString(path)));
            if (material == null || !material.isBlock() || !material.isItem())
                warn(path, "'" + c.getString(path) + "' is not a placeable block, using the default");
        }
    }

    private void warn(String path, String problem){
        plugin.getLogger().warning("config.yml -> " + path + ": " + problem);
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String value){
        if (value == null) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
