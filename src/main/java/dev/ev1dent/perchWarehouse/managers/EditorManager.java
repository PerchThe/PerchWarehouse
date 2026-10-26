package dev.ev1dent.perchWarehouse.managers;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.ConfigManager;
import dev.ev1dent.perchWarehouse.data.MarkerType;
import dev.ev1dent.perchWarehouse.data.SerializableLocation;
import dev.ev1dent.perchWarehouse.data.Warehouse;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * /warehouse editor. Backs up the admin's inventory to disk, hands out marker blocks, and shows the
 * existing markers as glowing block displays that only editors can see. Placing a marker adds it to
 * warehouses/<name>.yml, punching a marker removes it.
 *
 * Markers are display entities instead of real blocks so nothing is left in the arena if the server crashes.
 */
public class EditorManager {

    private static final String EXIT_TAG = "EXIT";

    private final WarehousePlugin plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey entityKey;
    private final File backupFolder;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Set<UUID> pendingRender = new HashSet<>();

    private static class Session {
        final UUID player;
        Warehouse warehouse;
        final List<Entity> entities = new ArrayList<>();

        Session(UUID player, Warehouse warehouse) {
            this.player = player;
            this.warehouse = warehouse;
        }
    }

    public EditorManager(WarehousePlugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "editor_item");
        this.entityKey = new NamespacedKey(plugin, "editor_marker");
        this.backupFolder = new File(plugin.getDataFolder(), "editor-backups");
    }

    private ConfigManager config() { return plugin.getConfigManager(); }

    public boolean isEditing(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public boolean isBeingEdited(String warehouseName) {
        return sessions.values().stream().anyMatch(s -> s.warehouse.getName().equals(warehouseName));
    }

    // =====================================================================================
    // enter / exit
    // =====================================================================================

    public void enter(Player player, Warehouse warehouse) {
        if (isEditing(player)) {
            Utils.send(player, "editor-already-editing");
            return;
        }
        WarehouseManager event = plugin.getWarehouseManager();
        if (event.isParticipant(player)) {
            Utils.send(player, "editor-in-event");
            return;
        }
        if (event.getState().isInProgress() && event.getActiveWarehouse() != null
            && event.getActiveWarehouse().getName().equals(warehouse.getName())) {
            Utils.send(player, "editor-warehouse-in-use");
            return;
        }
        // back up first. if that fails, don't touch their inventory at all.
        if (!writeBackup(player)) {
            Utils.send(player, "editor-backup-failed");
            return;
        }

        player.getInventory().clear();
        player.getInventory().setItem(0, createMarkerItem(MarkerType.PLAYER_SPAWN));
        player.getInventory().setItem(1, createMarkerItem(MarkerType.GUARANTEED));
        player.getInventory().setItem(2, createMarkerItem(MarkerType.RANDOM));
        player.getInventory().setItem(8, createExitItem());
        player.getInventory().setHeldItemSlot(0);
        if (config().isEditorCreative()) player.setGameMode(GameMode.CREATIVE);

        Session session = new Session(player.getUniqueId(), warehouse);
        sessions.put(player.getUniqueId(), session);
        render(session);
        Utils.send(player, "editor-entered", warehouse.getName(),
            warehouse.getMarkers(MarkerType.PLAYER_SPAWN).size(),
            warehouse.getMarkers(MarkerType.GUARANTEED).size(),
            warehouse.getMarkers(MarkerType.RANDOM).size());
    }

    /** Leaves the editor and gives the inventory back. */
    public boolean exit(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) return false;
        clearEntities(session);
        restoreBackup(player);
        Utils.send(player, "editor-exited", session.warehouse.getName());
        return true;
    }

    public void exitAll() {
        for (UUID uuid : new ArrayList<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) exit(player);
            else sessions.remove(uuid);
        }
    }

    /** After /warehouse reload the Warehouse objects are new, so point sessions at them (or close them). */
    public void rebind() {
        for (Session session : new ArrayList<>(sessions.values())) {
            Warehouse fresh = plugin.getTierManager().getWarehouse(session.warehouse.getName());
            Player player = Bukkit.getPlayer(session.player);
            if (fresh == null) {
                if (player != null) exit(player);
                continue;
            }
            session.warehouse = fresh;
            render(session);
        }
    }

    // =====================================================================================
    // inventory backup (on disk, so it survives a crash)
    // =====================================================================================

    private File backupFile(UUID uuid) {
        return new File(backupFolder, uuid + ".yml");
    }

    private boolean writeBackup(Player player) {
        if (!backupFolder.exists() && !backupFolder.mkdirs()) return false;
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("name", player.getName());
        yml.set("gamemode", player.getGameMode().name());
        yml.set("inventory", Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(player.getInventory().getContents())));
        try {
            yml.save(backupFile(player.getUniqueId()));
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not back up " + player.getName() + "'s inventory: " + e.getMessage());
            return false;
        }
    }

    public boolean hasBackup(Player player) {
        return backupFile(player.getUniqueId()).exists();
    }

    /** Restores and deletes the backup. Also used on join if the server crashed while they were editing. */
    public boolean restoreBackup(Player player) {
        File file = backupFile(player.getUniqueId());
        if (!file.exists()) return false;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        try {
            ItemStack[] items = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(yml.getString("inventory", "")));
            player.getInventory().clear();
            player.getInventory().setContents(items);
            GameMode mode = ConfigManager.parseEnum(GameMode.class, yml.getString("gamemode"));
            if (mode != null) player.setGameMode(mode);
        } catch (RuntimeException e) {
            // keep the file so nothing is lost, an admin can look at it
            plugin.getLogger().severe("Could not restore " + player.getName() + "'s editor backup (" + file.getName() + "): " + e.getMessage());
            return false;
        }
        if (!file.delete()) plugin.getLogger().warning("Could not delete " + file.getName() + " after restoring it");
        return true;
    }

    // =====================================================================================
    // items
    // =====================================================================================

    public ItemStack createMarkerItem(MarkerType type) {
        ConfigurationSection section = config().getSection("editor.markers." + type.getConfigKey());
        ItemStack item = new ItemStack(getMarkerMaterial(type));
        ItemMeta meta = item.getItemMeta();
        if (section != null) {
            meta.displayName(Utils.itemText(section.getString("name", type.getDisplayName())));
            List<Component> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) lore.add(Utils.itemText(line));
            meta.lore(lore);
        }
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createExitItem() {
        ConfigurationSection section = config().getSection("editor.exit-item");
        Material material = section == null ? null : Material.matchMaterial(String.valueOf(section.getString("material")));
        if (material == null || !material.isItem() || material.isAir()) material = Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (section != null) {
            meta.displayName(Utils.itemText(section.getString("name", "Exit Editor")));
            List<Component> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) lore.add(Utils.itemText(line));
            meta.lore(lore);
        }
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, EXIT_TAG);
        item.setItemMeta(meta);
        return item;
    }

    private Material getMarkerMaterial(MarkerType type) {
        Material material = Material.matchMaterial(String.valueOf(config().getString("editor.markers." + type.getConfigKey() + ".material")));
        if (material != null && material.isBlock() && material.isItem() && !material.isAir()) return material;
        return switch (type) {
            case PLAYER_SPAWN -> Material.LIME_CONCRETE;
            case GUARANTEED -> Material.GOLD_BLOCK;
            case RANDOM -> Material.LIGHT_BLUE_CONCRETE;
        };
    }

    private Color getGlowColor(MarkerType type) {
        try {
            return Utils.parseColor(config().getString("editor.markers." + type.getConfigKey() + ".glow-color"));
        } catch (IllegalArgumentException e) {
            return Color.WHITE;
        }
    }

    /** null if this isn't a marker item */
    public MarkerType getMarkerType(ItemStack item) {
        String tag = getTag(item);
        if (tag == null || tag.equals(EXIT_TAG)) return null;
        try {
            return MarkerType.valueOf(tag);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isExitItem(ItemStack item) {
        return EXIT_TAG.equals(getTag(item));
    }

    /** marker or exit item */
    public boolean isEditorItem(ItemStack item) {
        return getTag(item) != null;
    }

    private String getTag(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    // =====================================================================================
    // placing / removing markers
    // =====================================================================================

    public void placeMarker(Player player, Block block, MarkerType type) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        Warehouse warehouse = session.warehouse;

        World world = warehouse.getWorld();
        if (world == null || !world.equals(block.getWorld())) {
            if (warehouse.getTotalMarkers() == 0) {
                // first marker decides which world this warehouse is in
                warehouse.setWorldName(block.getWorld().getName());
            } else {
                Utils.send(player, "editor-wrong-world", warehouse.getWorldName());
                return;
            }
        }

        MarkerType existing = warehouse.getMarkerAt(block.getX(), block.getY(), block.getZ());
        if (existing != null) {
            Utils.send(player, "editor-marker-exists", existing.getDisplayName());
            return;
        }

        SerializableLocation location = type == MarkerType.PLAYER_SPAWN
            ? SerializableLocation.ofSpawn(block, player.getLocation().getYaw())
            : SerializableLocation.ofBlock(block);
        warehouse.getMarkers(type).add(location);
        plugin.getTierManager().save(warehouse);
        renderAllFor(warehouse);
        Utils.send(player, "editor-marker-added", type.getDisplayName(), warehouse.getMarkers(type).size());
    }

    /** Called when an editor punches one of the marker entities. Returns true if it was a marker. */
    public boolean removeMarker(Player player, Entity entity) {
        String tag = entity.getPersistentDataContainer().get(entityKey, PersistentDataType.STRING);
        if (tag == null) return false;
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return true;

        String[] split = tag.split("\\|", 2);
        MarkerType type;
        SerializableLocation target;
        try {
            type = MarkerType.valueOf(split[0]);
            target = SerializableLocation.parse(split[1]);
        } catch (RuntimeException e) {
            return true;
        }
        Warehouse warehouse = session.warehouse;
        boolean removed = warehouse.getMarkers(type).removeIf(loc -> loc.sameBlock(target));
        if (removed) {
            plugin.getTierManager().save(warehouse);
            renderAllFor(warehouse);
            Utils.send(player, "editor-marker-removed", type.getDisplayName(), warehouse.getMarkers(type).size());
        }
        return true;
    }

    public boolean isMarkerEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(entityKey, PersistentDataType.STRING);
    }

    // =====================================================================================
    // rendering
    // =====================================================================================

    private void renderAllFor(Warehouse warehouse) {
        for (Session session : sessions.values()) {
            if (session.warehouse.getName().equals(warehouse.getName())) render(session);
        }
    }

    private void clearEntities(Session session) {
        for (Entity entity : session.entities) {
            if (entity.isValid()) entity.remove();
        }
        session.entities.clear();
    }

    private void render(Session session) {
        clearEntities(session);
        Player viewer = Bukkit.getPlayer(session.player);
        World world = session.warehouse.getWorld();
        if (viewer == null || world == null) return;

        Map<MarkerType, Material> materials = new EnumMap<>(MarkerType.class);
        for (MarkerType type : MarkerType.values()) materials.put(type, getMarkerMaterial(type));

        for (MarkerType type : MarkerType.values()) {
            List<SerializableLocation> list = session.warehouse.getMarkers(type);
            for (int i = 0; i < list.size(); i++) {
                SerializableLocation loc = list.get(i);
                // only spawn into loaded chunks, the rest are drawn when their chunk loads
                if (!world.isChunkLoaded(loc.blockX() >> 4, loc.blockZ() >> 4)) continue;
                spawnMarker(session, viewer, world, type, loc, i + 1, materials.get(type));
            }
        }
    }

    private void spawnMarker(Session session, Player viewer, World world, MarkerType type, SerializableLocation loc, int number, Material material) {
        String tag = type.name() + "|" + loc.serializeBlock();
        Location corner = new Location(world, loc.blockX(), loc.blockY(), loc.blockZ());
        Color glow = getGlowColor(type);

        BlockDisplay display = world.spawn(corner, BlockDisplay.class, e -> {
            e.setPersistent(false);
            e.setVisibleByDefault(false);
            e.setBlock(material.createBlockData());
            e.setGlowing(true);
            e.setGlowColorOverride(glow);
            // a bit smaller than a full block so it's obvious it's a marker
            e.setTransformation(new Transformation(new Vector3f(0.15f, 0.15f, 0.15f), new AxisAngle4f(), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
            e.getPersistentDataContainer().set(entityKey, PersistentDataType.STRING, tag);
        });

        String facing = type == MarkerType.PLAYER_SPAWN ? " " + Utils.parsePlaceholders("editor-facing", facingName(loc.yaw())) : "";
        TextDisplay label = world.spawn(corner.clone().add(0.5, 1.15, 0.5), TextDisplay.class, e -> {
            e.setPersistent(false);
            e.setVisibleByDefault(false);
            e.setBillboard(Display.Billboard.CENTER);
            e.text(Utils.format("editor-marker-label", type.getDisplayName(), number, facing));
            e.getPersistentDataContainer().set(entityKey, PersistentDataType.STRING, tag);
        });

        // invisible hitbox so the marker can be punched to remove it
        Interaction hitbox = world.spawn(corner.clone().add(0.5, 0, 0.5), Interaction.class, e -> {
            e.setPersistent(false);
            e.setVisibleByDefault(false);
            e.setInteractionWidth(1f);
            e.setInteractionHeight(1f);
            e.setResponsive(true);
            e.getPersistentDataContainer().set(entityKey, PersistentDataType.STRING, tag);
        });

        for (Entity entity : new Entity[]{display, label, hitbox}) {
            viewer.showEntity(plugin, entity);
            session.entities.add(entity);
        }
    }

    private static String facingName(float yaw) {
        String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int index = Math.floorMod(Math.round(yaw / 45f), 8);
        return names[index];
    }

    /** Markers in unloaded chunks are drawn once the chunk loads. */
    public void handleChunkLoad(Chunk chunk) {
        if (sessions.isEmpty()) return;
        for (Session session : sessions.values()) {
            Warehouse warehouse = session.warehouse;
            if (!chunk.getWorld().getName().equals(warehouse.getWorldName())) continue;
            boolean hasMarker = false;
            for (MarkerType type : MarkerType.values()) {
                for (SerializableLocation loc : warehouse.getMarkers(type)) {
                    if (loc.blockX() >> 4 == chunk.getX() && loc.blockZ() >> 4 == chunk.getZ()) {
                        hasMarker = true;
                        break;
                    }
                }
            }
            if (hasMarker && pendingRender.add(session.player)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    pendingRender.remove(session.player);
                    if (sessions.get(session.player) == session) render(session);
                });
            }
        }
    }
}
