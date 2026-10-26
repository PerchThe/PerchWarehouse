package dev.ev1dent.perchWarehouse.managers;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.exceptions.QueueClosedException;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * The join queue. Membership lives in queue.yml (not the player's session), so a player who
 * relogs, or a server that restarts, keeps the queue as it was while it is open.
 */
public class QueueManager {

    private final WarehousePlugin plugin;
    private final File queueFile;
    private final LinkedHashSet<UUID> playerQueue = new LinkedHashSet<>();
    private boolean isOpened;
    private long closesAt;

    public QueueManager(WarehousePlugin plugin) {
        this.plugin = plugin;
        this.queueFile = new File(plugin.getDataFolder(), "queue.yml");
    }

    /**
     * @return true if they were added, false if they were already queued
     */
    public boolean addPlayer(Player player) throws QueueClosedException {
        if(!isOpened){
            throw new QueueClosedException();
        }
        Utils.getDebugLogger("adding " + player.getName() + " to the queue");
        if (!playerQueue.add(player.getUniqueId())) return false;
        save();
        return true;
    }

    /**
     * @return true if they were removed, false if they weren't queued
     */
    public boolean removePlayer(Player player) throws QueueClosedException {
        if(!isOpened){
            throw new QueueClosedException();
        }
        return removeUUID(player.getUniqueId());
    }

    /** Removes without the open check, used for kicks/bans. */
    public boolean removeUUID(UUID uuid) {
        if (!playerQueue.remove(uuid)) return false;
        Utils.getDebugLogger("removed " + uuid + " from the queue");
        save();
        return true;
    }

    public boolean isQueued(Player player) {
        return playerQueue.contains(player.getUniqueId());
    }

    public int getQueueSize() {
        return playerQueue.size();
    }

    public boolean isOpen() {
        return isOpened;
    }

    public long getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(long closesAt) {
        this.closesAt = closesAt;
        save();
    }

    public void open(long closesAt) {
        this.isOpened = true;
        this.closesAt = closesAt;
        playerQueue.clear();
        save();
    }

    /** Closes and clears the queue. */
    public void close() {
        this.isOpened = false;
        this.closesAt = 0;
        playerQueue.clear();
        save();
    }

    public void clearQueue() {
        playerQueue.clear();
        save();
    }

    /** Players in the order they joined. */
    public LinkedList<UUID> getPlayersInQueue() {
        return new LinkedList<>(playerQueue);
    }

    // ------------------------------------------------------------------ persistence

    /** Loads queue.yml. Returns true if the queue was open when the server stopped. */
    public boolean load() {
        playerQueue.clear();
        if (!queueFile.exists()) return false;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(queueFile);
        this.isOpened = yml.getBoolean("open", false);
        this.closesAt = yml.getLong("closes-at", 0);
        for (String raw : yml.getStringList("players")) {
            try {
                playerQueue.add(UUID.fromString(raw));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("queue.yml -> players: '" + raw + "' is not a UUID, skipping");
            }
        }
        return isOpened;
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("open", isOpened);
        yml.set("closes-at", closesAt);
        List<String> raw = new ArrayList<>();
        for (UUID uuid : playerQueue) raw.add(uuid.toString());
        yml.set("players", raw);
        try {
            yml.save(queueFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save queue.yml: " + e.getMessage());
        }
    }
}
