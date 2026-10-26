package dev.ev1dent.perchWarehouse.managers;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.data.BlockKey;
import dev.ev1dent.perchWarehouse.data.EventState;
import dev.ev1dent.perchWarehouse.data.PlayerSnapshot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * state.yml: the current state, when the next event starts, and everything needed to clean up
 * if the server dies mid-event (where players came from, and which blocks we replaced).
 */
public class StateStore {

    private final WarehousePlugin plugin;
    private final File file;

    public EventState state = EventState.IDLE;
    public long nextEventAt = 0;
    /** players who still need to be sent back. survives restarts so offline players are fixed on join. */
    public final Map<UUID, PlayerSnapshot> returns = new LinkedHashMap<>();
    /** boxes in the world -> the block data that was there before */
    public final Map<BlockKey, String> boxes = new LinkedHashMap<>();

    public StateStore(WarehousePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
    }

    public void load() {
        returns.clear();
        boxes.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        try {
            state = EventState.valueOf(yml.getString("state", "IDLE"));
        } catch (IllegalArgumentException e) {
            state = EventState.IDLE;
        }
        nextEventAt = yml.getLong("next-event-at", 0);

        ConfigurationSection returnSection = yml.getConfigurationSection("returns");
        if (returnSection != null) {
            for (String key : returnSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    returns.put(uuid, PlayerSnapshot.load(uuid, returnSection.getConfigurationSection(key)));
                } catch (IllegalArgumentException | NullPointerException e) {
                    plugin.getLogger().warning("state.yml -> returns." + key + ": could not be read, skipping");
                }
            }
        }
        ConfigurationSection boxSection = yml.getConfigurationSection("boxes");
        if (boxSection != null) {
            for (String key : boxSection.getKeys(false)) {
                ConfigurationSection entry = boxSection.getConfigurationSection(key);
                if (entry == null) continue;
                try {
                    boxes.put(BlockKey.parse(entry.getString("block", "")), entry.getString("previous", "minecraft:air"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("state.yml -> boxes." + key + ": " + e.getMessage());
                }
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("state", state.name());
        yml.set("next-event-at", nextEventAt);
        for (Map.Entry<UUID, PlayerSnapshot> entry : returns.entrySet()) {
            entry.getValue().save(yml.createSection("returns." + entry.getKey()));
        }
        int i = 0;
        for (Map.Entry<BlockKey, String> entry : boxes.entrySet()) {
            ConfigurationSection section = yml.createSection("boxes." + (i++));
            section.set("block", entry.getKey().serialize());
            section.set("previous", entry.getValue());
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save state.yml: " + e.getMessage());
        }
    }
}
