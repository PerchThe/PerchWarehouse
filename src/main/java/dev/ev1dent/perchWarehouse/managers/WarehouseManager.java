package dev.ev1dent.perchWarehouse.managers;

import dev.ev1dent.perchWarehouse.Events.QueueOpenEvent;
import dev.ev1dent.perchWarehouse.Events.WarehouseEndEvent;
import dev.ev1dent.perchWarehouse.Events.WarehouseStartEvent;
import dev.ev1dent.perchWarehouse.IUser;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.ConfigManager;
import dev.ev1dent.perchWarehouse.configuration.LootManager;
import dev.ev1dent.perchWarehouse.configuration.TierManager;
import dev.ev1dent.perchWarehouse.data.BlockKey;
import dev.ev1dent.perchWarehouse.data.EventState;
import dev.ev1dent.perchWarehouse.data.Rarity;
import dev.ev1dent.perchWarehouse.data.MarkerType;
import dev.ev1dent.perchWarehouse.data.PlacedBox;
import dev.ev1dent.perchWarehouse.data.PlayerSnapshot;
import dev.ev1dent.perchWarehouse.data.SerializableLocation;
import dev.ev1dent.perchWarehouse.data.Warehouse;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The event state machine. Exactly one {@link EventState} at a time, and nothing outside this class changes it.
 * Everything is driven by one repeating task once a second, so timers can't drift apart.
 */
public class WarehouseManager {

    private static final long LOCKING_TIMEOUT_MILLIS = 30_000;
    private static final long REBOOT_QUEUE_GRACE_MILLIS = 30_000;

    private final WarehousePlugin plugin;
    private final QueueManager queueManager;
    private final StateStore store;

    private final NamespacedKey boxKey;
    private final NamespacedKey fireworkKey;
    private final NamespacedKey freezeKey;

    private EventState state = EventState.IDLE;
    private BukkitTask driver;
    /** bumped every time an event ends, so late callbacks from an old event are ignored */
    private int generation = 0;
    private boolean shuttingDown = false;

    private Warehouse activeWarehouse;
    /** players currently inside the event -> where to send them back */
    private final Map<UUID, PlayerSnapshot> participants = new LinkedHashMap<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Map<BlockKey, PlacedBox> boxes = new LinkedHashMap<>();
    /** boxes picked during locking, placed when the countdown ends */
    private final List<PendingCrate> pendingBoxes = new ArrayList<>();
    private final List<Chunk> ticketChunks = new ArrayList<>();
    private final List<Location> spawnPoints = new ArrayList<>();

    private long queueTotalMillis;
    private final Set<Integer> announced = new HashSet<>();
    private long lockingStartedAt;
    private int countdownLeft;
    private long endsAt;
    private BossBar queueBar;
    private BossBar eventBar;

    public WarehouseManager(WarehousePlugin plugin) {
        this.plugin = plugin;
        this.queueManager = plugin.getQueueManager();
        this.store = new StateStore(plugin);
        this.boxKey = new NamespacedKey(plugin, "warehouse_box");
        this.fireworkKey = new NamespacedKey(plugin, "warehouse_firework");
        this.freezeKey = new NamespacedKey(plugin, "warehouse_freeze");
    }

    private ConfigManager config() { return plugin.getConfigManager(); }
    private TierManager tiers() { return plugin.getTierManager(); }
    private LootManager loot() { return plugin.getLootManager(); }

    // =====================================================================================
    // lifecycle
    // =====================================================================================

    public void enable() {
        // wait one tick so worlds are loaded (plugin.yml uses load: STARTUP)
        Bukkit.getScheduler().runTask(plugin, this::recover);
        driver = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void disable() {
        shuttingDown = true;
        forceEnd("event-ended-shutdown");
        if (driver != null) driver.cancel();
        // the queue stays in queue.yml so it reopens after the restart
    }

    /**
     * Runs once after startup. If the server died mid-event, clean up instead of coming back half-running.
     */
    private void recover() {
        store.load();
        boolean queueWasOpen = queueManager.load();
        long now = System.currentTimeMillis();
        EventState saved = store.state;

        if (saved.isInProgress() || saved == EventState.CLEANUP) {
            plugin.getLogger().warning("The server stopped during a warehouse event (" + saved + "). Cleaning up leftover boxes; players are sent back when they log in.");
            for (Map.Entry<BlockKey, String> entry : new ArrayList<>(store.boxes.entrySet())) {
                if (restoreBlock(entry.getKey(), entry.getValue())) store.boxes.remove(entry.getKey());
            }
            queueManager.close();
            store.state = EventState.IDLE;
            store.nextEventAt = now + config().getIntervalMillis();
        } else if (queueWasOpen) {
            long closesAt = Math.max(queueManager.getClosesAt(), now + REBOOT_QUEUE_GRACE_MILLIS);
            queueManager.setClosesAt(closesAt);
            queueTotalMillis = closesAt - now;
            markAnnounced(queueTotalMillis);
            state = EventState.QUEUE_OPEN;
            store.state = EventState.QUEUE_OPEN;
            Utils.getInfoLogger("Reopened the warehouse queue from before the restart (" + queueManager.getQueueSize() + " queued).");
            if (config().isQueueBossBar()) {
                queueBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
                Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(queueBar));
            }
        } else {
            store.state = EventState.IDLE;
        }

        // don't start an event the instant the server boots just because it was offline for a while
        if (store.nextEventAt <= 0) store.nextEventAt = now + config().getIntervalMillis();
        else if (store.nextEventAt < now + 60_000) store.nextEventAt = now + 60_000;
        store.save();

        // players already online (e.g. after /reload) who still need to be sent back
        for (Player player : Bukkit.getOnlinePlayers()) handleJoin(player);
    }

    // =====================================================================================
    // the driving task
    // =====================================================================================

    private void tick() {
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> {
                if (config().isAutoStart() && store.nextEventAt > 0 && now >= store.nextEventAt) {
                    String problem = openRegistration(config().getQueueOpenSeconds());
                    if (problem != null) {
                        plugin.getLogger().warning("Couldn't open the warehouse queue automatically: " + Utils.parsePlaceholders(problem));
                        store.nextEventAt = now + config().getIntervalMillis();
                        store.save();
                    }
                }
            }
            case QUEUE_OPEN -> tickQueue(now);
            case LOCKING -> {
                if (now - lockingStartedAt > LOCKING_TIMEOUT_MILLIS) {
                    plugin.getLogger().warning("Teleports took longer than " + (LOCKING_TIMEOUT_MILLIS / 1000) + "s, starting the countdown anyway.");
                    beginCountdown();
                }
            }
            case COUNTDOWN -> {
                countdownLeft--;
                if (countdownLeft <= 0) beginRunning();
                else showCountdown();
            }
            case RUNNING -> {
                updateEventBar(now);
                if (now >= endsAt) forceEnd("event-ended-time");
            }
            case CLEANUP -> { }
        }
    }

    private void tickQueue(long now) {
        long leftMillis = queueManager.getClosesAt() - now;
        long secondsLeft = (long) Math.ceil(Math.max(0, leftMillis) / 1000.0);

        for (int at : config().getAnnounceAt()) {
            if (secondsLeft <= at && announced.add(at) && secondsLeft > 0) {
                Bukkit.broadcast(Utils.format("queue-closing-soon", secondsLeft, queueManager.getQueueSize()));
            }
        }
        if (queueBar != null) {
            queueBar.name(Utils.format("queue-bossbar", Utils.formatTime(secondsLeft), queueManager.getQueueSize()));
            queueBar.progress(clamp(queueTotalMillis <= 0 ? 0 : (float) leftMillis / queueTotalMillis));
        }
        Component actionBar = Utils.format("queue-actionbar", queueManager.getQueueSize(), Utils.formatTime(secondsLeft));
        for (UUID uuid : queueManager.getPlayersInQueue()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendActionBar(actionBar);
        }
        if (leftMillis <= 0) closeQueue();
    }

    // =====================================================================================
    // QUEUE_OPEN
    // =====================================================================================

    /**
     * Opens the queue. Returns null if it opened, otherwise a messages.yml key explaining why not.
     */
    public String openRegistration(int seconds) {
        if (state != EventState.IDLE) return "event-already-running";
        boolean anyReady = tiers().getWarehouses().stream().anyMatch(Warehouse::isReady);
        if (!anyReady) return "no-warehouse-ready";

        long now = System.currentTimeMillis();
        queueTotalMillis = seconds * 1000L;
        queueManager.open(now + queueTotalMillis);
        announced.clear();
        markAnnounced(queueTotalMillis);
        setState(EventState.QUEUE_OPEN);
        Utils.getDebugLogger("Queue opened for " + seconds + " seconds");

        Bukkit.broadcast(Utils.format("queue-opened", Utils.formatTime(seconds)));
        if (config().isQueueBossBar()) {
            queueBar = BossBar.bossBar(Utils.format("queue-bossbar", Utils.formatTime(seconds), 0), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(queueBar));
        }
        Bukkit.getPluginManager().callEvent(new QueueOpenEvent(seconds));
        return null;
    }

    /** reminders longer than the queue itself shouldn't all fire on the first tick */
    private void markAnnounced(long totalMillis) {
        for (int at : config().getAnnounceAt()) {
            if (at * 1000L >= totalMillis) announced.add(at);
        }
    }

    /**
     * Closes the queue right now and starts the event (used by the timer and /warehouse forcestart).
     * Returns null if it worked, otherwise a messages.yml key.
     */
    public String start() {
        if (state != EventState.QUEUE_OPEN) return "queue-not-open";
        closeQueue();
        return null;
    }

    private void closeQueue() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : queueManager.getPlayersInQueue()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !player.isDead() && !plugin.getEditorManager().isEditing(player)) players.add(player);
        }
        queueManager.close();
        hideQueueBar();
        Utils.getDebugLogger("Queue closed with " + players.size() + " online players");

        if (players.size() < config().getMinPlayers()) {
            Bukkit.broadcast(Utils.format("not-enough-players", players.size(), config().getMinPlayers()));
            finishToIdle();
            return;
        }

        Utils.getDebugLogger("Checking Warehouse Tier");
        Warehouse warehouse = tiers().getTierForPlayerCount(players.size());
        if (warehouse == null) {
            Bukkit.broadcast(Utils.format("no-warehouse-ready"));
            finishToIdle();
            return;
        }
        Utils.getDebugLogger("Warehouse Tier is " + warehouse.getName() + " for " + players.size() + " players");
        beginLocking(warehouse, players);
    }

    // =====================================================================================
    // LOCKING
    // =====================================================================================

    private void beginLocking(Warehouse warehouse, List<Player> players) {
        World world = warehouse.getWorld();
        activeWarehouse = warehouse;
        lockingStartedAt = System.currentTimeMillis();
        setStateNoSave(EventState.LOCKING);
        final int gen = generation;

        // spawns: shuffled for fairness, then handed out round-robin. if there are more players
        // than spawns (shouldn't happen with the tiers), people share.
        spawnPoints.clear();
        List<SerializableLocation> spawns = new ArrayList<>(warehouse.getMarkers(MarkerType.PLAYER_SPAWN));
        Collections.shuffle(spawns);
        for (SerializableLocation spawn : spawns) spawnPoints.add(spawn.toLocation(world));

        pickBoxes(warehouse);

        // snapshot + save BEFORE teleporting, so a crash from here on can still send everyone back
        for (Player player : players) {
            PlayerSnapshot snapshot = PlayerSnapshot.of(player);
            participants.put(player.getUniqueId(), snapshot);
            store.returns.put(player.getUniqueId(), snapshot);
        }
        store.save();

        List<CompletableFuture<?>> futures = new ArrayList<>();

        // load the box chunks now and keep them loaded for the whole event
        Set<Long> chunkKeys = new HashSet<>();
        for (PendingCrate pending : pendingBoxes) {
            int cx = pending.location().blockX() >> 4;
            int cz = pending.location().blockZ() >> 4;
            if (chunkKeys.add(Chunk.getChunkKey(cx, cz))) {
                futures.add(world.getChunkAtAsync(cx, cz).thenAccept(chunk -> {
                    chunk.addPluginChunkTicket(plugin);
                    ticketChunks.add(chunk);
                }));
            }
        }

        final org.bukkit.GameMode eventMode = config().getEventGameMode();
        int i = 0;
        for (Player player : players) {
            freeze(player);
            Location spawn = spawnPoints.get(i++ % spawnPoints.size());
            PlayerSnapshot snapshot = participants.get(player.getUniqueId());
            futures.add(IUser.getUser(player).teleportAsync(spawn).thenAccept(success -> {
                if (!success) {
                    plugin.getLogger().warning("Teleporting " + player.getName() + " into the warehouse failed.");
                } else if (gen == generation && participants.containsKey(player.getUniqueId())) {
                    player.setGameMode(eventMode);
                } else if (player.isOnline() && !participants.containsKey(player.getUniqueId())) {
                    // the event was stopped (or they left) while this teleport was still loading the chunk,
                    // so it landed after we already sent them back. send them back again.
                    restorePlayer(player, snapshot);
                }
            }));
            Utils.send(player, "teleporting-to-warehouse");
        }

        // only start the countdown once every teleport AND chunk load has finished.
        // starting early can unfreeze someone inside a wall.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((ignored, error) -> {
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (gen != generation || state != EventState.LOCKING) return;
                if (error != null) plugin.getLogger().warning("Something went wrong while teleporting players: " + error.getMessage());
                beginCountdown();
            });
        });
    }

    /** A crate picked during locking, placed when the countdown ends. */
    private record PendingCrate(SerializableLocation location, MarkerType spot, Warehouse.CrateType crate, Rarity rarity) {}

    /**
     * Decides where crates go and which rarity each one is:
     * 1. spots = every guaranteed spot + "random-spawns" of the random spots, picked fresh each event
     * 2. every rarity's minimum-crate-spawns is handed out first (rarest first, in case there aren't enough spots)
     * 3. the rest are picked by weight, skipping rarities that hit maximum-crate-spawns
     * 4. the rarities are shuffled across the spots, so minimums don't always land in the same place
     */
    private void pickBoxes(Warehouse warehouse) {
        pendingBoxes.clear();

        List<Map.Entry<SerializableLocation, MarkerType>> spots = new ArrayList<>();
        List<SerializableLocation> guaranteed = warehouse.getMarkers(MarkerType.GUARANTEED);
        for (SerializableLocation loc : guaranteed) spots.add(Map.entry(loc, MarkerType.GUARANTEED));
        List<SerializableLocation> randoms = new ArrayList<>();
        for (SerializableLocation loc : warehouse.getMarkers(MarkerType.RANDOM)) {
            if (guaranteed.stream().noneMatch(g -> g.sameBlock(loc))) randoms.add(loc);
        }
        Collections.shuffle(randoms);
        int randomAmount = Math.min(warehouse.getRandomSpawns(), randoms.size());
        for (int i = 0; i < randomAmount; i++) spots.add(Map.entry(randoms.get(i), MarkerType.RANDOM));

        // only rarities that actually have a loot file
        List<Warehouse.CrateType> usable = new ArrayList<>();
        for (Warehouse.CrateType type : warehouse.getCrateTypes()) {
            if (loot().getRarity(type.rarity()) != null) usable.add(type);
            else plugin.getLogger().warning("warehouses/" + warehouse.getName() + ".yml: there is no loot/" + type.rarity() + ".yml, skipping that crate");
        }

        Map<Warehouse.CrateType, Integer> counts = new HashMap<>();
        List<Warehouse.CrateType> assigned = new ArrayList<>();

        List<Warehouse.CrateType> byRarest = new ArrayList<>(usable);
        byRarest.sort(Comparator.comparingInt(Warehouse.CrateType::weight));
        for (Warehouse.CrateType type : byRarest) {
            for (int i = 0; i < type.minimum() && assigned.size() < spots.size(); i++) {
                assigned.add(type);
                counts.merge(type, 1, Integer::sum);
            }
        }

        while (assigned.size() < spots.size()) {
            List<Warehouse.CrateType> candidates = new ArrayList<>();
            int totalWeight = 0;
            for (Warehouse.CrateType type : usable) {
                if (type.weight() <= 0) continue;
                if (type.hasMaximum() && counts.getOrDefault(type, 0) >= type.maximum()) continue;
                candidates.add(type);
                totalWeight += type.weight();
            }
            if (candidates.isEmpty()) break; // everything is at its maximum, leave the remaining spots empty
            int roll = ThreadLocalRandom.current().nextInt(totalWeight);
            for (Warehouse.CrateType type : candidates) {
                roll -= type.weight();
                if (roll < 0) {
                    assigned.add(type);
                    counts.merge(type, 1, Integer::sum);
                    break;
                }
            }
        }

        Collections.shuffle(assigned);
        for (int i = 0; i < assigned.size(); i++) {
            Warehouse.CrateType type = assigned.get(i);
            Map.Entry<SerializableLocation, MarkerType> spot = spots.get(i);
            pendingBoxes.add(new PendingCrate(spot.getKey(), spot.getValue(), type, loot().getRarity(type.rarity())));
        }
        Utils.getDebugLogger("Picked " + pendingBoxes.size() + " crates for " + warehouse.getName() + ": " + counts);
    }

    // =====================================================================================
    // COUNTDOWN
    // =====================================================================================

    private void beginCountdown() {
        if (participants.isEmpty()) {
            forceEnd("event-ended-empty");
            return;
        }
        setState(EventState.COUNTDOWN);
        countdownLeft = config().getCountdownSeconds();
        if (countdownLeft <= 0) beginRunning();
        else showCountdown();
    }

    private void showCountdown() {
        Title title = Title.title(
            Utils.format("countdown-title", countdownLeft),
            Utils.format("countdown-subtitle", countdownLeft),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO));
        for (Player player : onlineParticipants()) {
            player.showTitle(title);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }
    }

    // =====================================================================================
    // RUNNING
    // =====================================================================================

    private void beginRunning() {
        List<Player> players = onlineParticipants();
        if (players.isEmpty()) {
            forceEnd("event-ended-empty");
            return;
        }
        Title go = Title.title(Utils.format("go-title"), Utils.format("go-subtitle"),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500)));
        for (Player player : players) {
            unfreeze(player);
            player.showTitle(go);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }
        frozen.clear();

        placeBoxes();
        endsAt = System.currentTimeMillis() + config().getDurationMillis();
        setState(EventState.RUNNING);

        long seconds = config().getDurationMillis() / 1000;
        for (Player player : players) Utils.send(player, "event-started", boxes.size(), Utils.formatTime(seconds));
        if (config().isEventBossBar()) {
            eventBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
            players.forEach(p -> p.showBossBar(eventBar));
            updateEventBar(System.currentTimeMillis());
        }
        Bukkit.getPluginManager().callEvent(new WarehouseStartEvent(activeWarehouse, players));
    }

    private void placeBoxes() {
        World world = activeWarehouse.getWorld();
        if (world == null) return;
        for (PendingCrate pending : pendingBoxes) {
            Block block = pending.location().toBlock(world);
            BlockKey key = BlockKey.of(block);
            if (boxes.containsKey(key)) continue;

            // never wipe out a real chest/barrel someone left on a marker
            if (block.getState() instanceof Container && !isOurBox(block)) {
                plugin.getLogger().warning("There is a container on the marker at " + key.serialize() + ", skipping that crate.");
                continue;
            }

            String previous = block.getBlockData().getAsString();
            block.setType(pending.crate().block(), false);
            BlockState blockState = block.getState();
            if (blockState instanceof TileState tile) {
                tile.getPersistentDataContainer().set(boxKey, PersistentDataType.STRING, activeWarehouse.getName() + ":" + pending.rarity().getName());
                tile.update(true, false);
            }
            boxes.put(key, new PlacedBox(key, pending.spot(), pending.rarity(), previous));
            store.boxes.put(key, previous);
        }
        pendingBoxes.clear();
        store.save();
        Utils.getDebugLogger("Placed " + boxes.size() + " boxes in " + activeWarehouse.getName());
    }

    private void updateEventBar(long now) {
        if (eventBar == null) return;
        long secondsLeft = (long) Math.ceil(Math.max(0, endsAt - now) / 1000.0);
        eventBar.name(Utils.format("event-bossbar", Utils.formatTime(secondsLeft), boxes.size()));
        eventBar.progress(clamp((float) (endsAt - now) / config().getDurationMillis()));
    }

    /**
     * Called when a player right-clicks a block. Returns true if it was one of our boxes (so the
     * listener cancels the event either way).
     */
    public boolean lootBox(Player player, Block block) {
        BlockKey key = BlockKey.of(block);
        PlacedBox box = boxes.get(key);
        if (box == null) return isOurBox(block);

        if (state != EventState.RUNNING) {
            Utils.send(player, "event-not-started");
            return true;
        }
        if (!participants.containsKey(player.getUniqueId())) {
            Utils.send(player, "not-in-event");
            return true;
        }

        boxes.remove(key);
        restoreBlock(key, box.previousBlock());
        store.boxes.remove(key);
        store.save();

        // everything inside the crate goes straight to the player
        boolean dropped = false;
        for (Rarity.Entry entry : box.rarity().rollContents()) {
            for (ItemStack stack : entry.createStacks()) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                    dropped = true;
                }
            }
            for (String command : entry.commands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
            }
            if (entry.message() != null) player.sendMessage(Utils.miniMessage(entry.message().replace("{player}", player.getName())));
        }
        if (dropped) Utils.send(player, "inventory-full");

        launchFirework(key.center(), box.rarity());
        for (Player p : onlineParticipants()) {
            Utils.send(p, "box-looted", player.getName(), box.rarity().getDisplayName(), boxes.size());
        }

        if (boxes.isEmpty()) {
            long early = System.currentTimeMillis() + config().getEndWhenLootedDelaySeconds() * 1000L;
            if (early < endsAt) endsAt = early;
            for (Player p : onlineParticipants()) Utils.send(p, "all-boxes-looted");
        }
        return true;
    }

    private void launchFirework(Location location, Rarity table) {
        World world = location.getWorld();
        if (world == null) return;
        if (table.getFirework() == null) {
            world.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
            return;
        }
        Firework firework = world.spawn(location, Firework.class, fw -> {
            FireworkMeta meta = fw.getFireworkMeta();
            meta.clearEffects();
            meta.addEffect(table.getFirework());
            meta.setPower(table.getFireworkPower());
            fw.setFireworkMeta(meta);
            fw.getPersistentDataContainer().set(fireworkKey, PersistentDataType.BYTE, (byte) 1);
        });
        firework.detonate();
    }

    public boolean isOurFirework(org.bukkit.entity.Entity entity) {
        return entity instanceof Firework && entity.getPersistentDataContainer().has(fireworkKey, PersistentDataType.BYTE);
    }

    // =====================================================================================
    // CLEANUP
    // =====================================================================================

    /** /warehouse forcestop. returns false if there was nothing to stop. */
    public boolean stop() {
        if (state == EventState.IDLE) return false;
        forceEnd("event-ended-staff");
        return true;
    }

    /**
     * Ends whatever is happening and puts everything back. Safe to call more than once and from
     * anywhere: the timer, /warehouse forcestop, and onDisable all use it.
     *
     * @param reasonKey messages.yml key sent to participants
     */
    public void forceEnd(String reasonKey) {
        if (state == EventState.CLEANUP) return;
        EventState was = state;
        state = EventState.CLEANUP;
        generation++;
        Utils.getDebugLogger("forceEnd(" + reasonKey + ") from " + was);

        hideQueueBar();
        hideEventBar();
        if (was == EventState.QUEUE_OPEN && !shuttingDown) Bukkit.broadcast(Utils.format("queue-cancelled"));
        if (queueManager.isOpen() && !shuttingDown) queueManager.close();

        for (Map.Entry<UUID, PlayerSnapshot> entry : new ArrayList<>(participants.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue; // offline: stays in store.returns, fixed when they log in
            Utils.send(player, reasonKey);
            if (restorePlayer(player, entry.getValue())) store.returns.remove(entry.getKey());
        }
        participants.clear();
        frozen.clear();
        pendingBoxes.clear();

        for (PlacedBox box : boxes.values()) {
            restoreBlock(box.key(), box.previousBlock());
            store.boxes.remove(box.key());
        }
        boxes.clear();
        for (Map.Entry<BlockKey, String> entry : new ArrayList<>(store.boxes.entrySet())) {
            if (restoreBlock(entry.getKey(), entry.getValue())) store.boxes.remove(entry.getKey());
        }

        for (Chunk chunk : ticketChunks) chunk.removePluginChunkTicket(plugin);
        ticketChunks.clear();
        spawnPoints.clear();

        Warehouse warehouse = activeWarehouse;
        activeWarehouse = null;
        // only push the timer back if something actually happened, otherwise every restart would reset it
        if (was == EventState.IDLE) {
            state = EventState.IDLE;
            store.state = EventState.IDLE;
            store.save();
            return;
        }
        finishToIdle();
        if (!shuttingDown) Bukkit.getPluginManager().callEvent(new WarehouseEndEvent(warehouse, reasonKey));
    }

    private void finishToIdle() {
        state = EventState.IDLE;
        store.state = EventState.IDLE;
        store.nextEventAt = System.currentTimeMillis() + config().getIntervalMillis();
        store.save();
    }

    /**
     * Sends a player back where they came from. Returns false if they're dead and need to be handled on respawn.
     */
    private boolean restorePlayer(Player player, PlayerSnapshot snapshot) {
        unfreeze(player);
        if (player.isDead()) return false;
        player.setFallDistance(0);
        player.setFireTicks(0);
        player.setGameMode(snapshot.gameMode());
        Location back = snapshot.toLocation();
        if (back == null) back = Bukkit.getWorlds().getFirst().getSpawnLocation();
        if (shuttingDown) player.teleport(back);
        else player.teleportAsync(back);
        return true;
    }

    /** Puts back whatever block was there before. false if the world isn't loaded. */
    private boolean restoreBlock(BlockKey key, String previous) {
        Block block = key.toBlock();
        if (block == null) return false;
        try {
            block.setBlockData(Bukkit.createBlockData(previous == null ? "minecraft:air" : previous), false);
        } catch (IllegalArgumentException e) {
            block.setType(org.bukkit.Material.AIR, false);
        }
        return true;
    }

    // =====================================================================================
    // player hooks (called from listeners)
    // =====================================================================================

    public void handleJoin(Player player) {
        unfreeze(player); // removes leftover modifiers if the server crashed while they were frozen
        if (queueBar != null) player.showBossBar(queueBar);

        UUID uuid = player.getUniqueId();
        PlayerSnapshot snapshot = store.returns.get(uuid);
        if (snapshot != null && !participants.containsKey(uuid)) {
            // a tick later, teleporting during join can be unreliable
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline() || participants.containsKey(uuid)) return;
                if (restorePlayer(player, snapshot)) {
                    store.returns.remove(uuid);
                    store.save();
                    Utils.send(player, "returned-after-event");
                }
            });
        }
        if (state == EventState.QUEUE_OPEN && queueManager.isQueued(player)) {
            Utils.send(player, "still-queued", Utils.formatTime((queueManager.getClosesAt() - System.currentTimeMillis()) / 1000));
        }
    }

    /** Logging out mid-event takes you out of it. You are sent back when you log in again. */
    public void handleQuit(Player player) {
        if (eventBar != null) player.hideBossBar(eventBar);
        if (queueBar != null) player.hideBossBar(queueBar);
        UUID uuid = player.getUniqueId();
        if (participants.remove(uuid) == null) return;
        frozen.remove(uuid);
        unfreeze(player);
        Utils.getDebugLogger(player.getName() + " left mid-event, will be returned on join");
        endIfEmptyLater();
    }

    /** /warehouse leave while in the event */
    public boolean leaveEvent(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerSnapshot snapshot = participants.remove(uuid);
        if (snapshot == null) return false;
        frozen.remove(uuid);
        if (eventBar != null) player.hideBossBar(eventBar);
        if (restorePlayer(player, snapshot)) store.returns.remove(uuid);
        store.save();
        endIfEmptyLater();
        return true;
    }

    public void handleDeath(Player player, org.bukkit.event.entity.PlayerDeathEvent event) {
        if (!isParticipant(player)) return;
        if (config().isKeepInventory()) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
        if (config().getDeathMode() == ConfigManager.DeathMode.ELIMINATE || !state.isInProgress()) {
            participants.remove(player.getUniqueId()); // snapshot stays in store.returns, used on respawn
            frozen.remove(player.getUniqueId());
            if (eventBar != null) player.hideBossBar(eventBar);
            Utils.send(player, "eliminated");
            endIfEmptyLater();
        }
    }

    /** Returns where this player should respawn, or null to leave it alone. */
    public Location handleRespawn(Player player) {
        UUID uuid = player.getUniqueId();
        if (participants.containsKey(uuid) && state.isInProgress() && !spawnPoints.isEmpty()) {
            return spawnPoints.get(ThreadLocalRandom.current().nextInt(spawnPoints.size()));
        }
        PlayerSnapshot snapshot = store.returns.get(uuid);
        if (snapshot == null || participants.containsKey(uuid)) return null;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (restorePlayer(player, snapshot)) {
                store.returns.remove(uuid);
                store.save();
            }
        });
        return snapshot.toLocation();
    }

    private void endIfEmptyLater() {
        if (!state.isInProgress()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (state.isInProgress() && onlineParticipants().isEmpty()) forceEnd("event-ended-empty");
        });
    }

    // =====================================================================================
    // freezing
    // =====================================================================================

    private void freeze(Player player) {
        frozen.add(player.getUniqueId());
        addModifier(player.getAttribute(Attribute.MOVEMENT_SPEED));
        addModifier(player.getAttribute(Attribute.JUMP_STRENGTH));
    }

    private void addModifier(AttributeInstance attribute) {
        if (attribute == null || attribute.getModifier(freezeKey) != null) return;
        attribute.addModifier(new AttributeModifier(freezeKey, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
    }

    public void unfreeze(Player player) {
        frozen.remove(player.getUniqueId());
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(freezeKey);
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) jump.removeModifier(freezeKey);
    }

    // =====================================================================================
    // queries
    // =====================================================================================

    public EventState getState() { return state; }
    public Warehouse getActiveWarehouse() { return activeWarehouse; }
    public long getNextEventAt() { return store.nextEventAt; }
    public long getEndsAt() { return endsAt; }
    public int getBoxesLeft() { return boxes.size(); }
    public Map<UUID, PlayerSnapshot> getParticipants() { return Collections.unmodifiableMap(participants); }

    public boolean isParticipant(Player player) {
        return participants.containsKey(player.getUniqueId());
    }

    public boolean isFrozen(Player player) {
        return state.isFrozen() && frozen.contains(player.getUniqueId());
    }

    public boolean isBox(Block block) {
        return boxes.containsKey(BlockKey.of(block));
    }

    /** true for tracked boxes AND leftover tagged boxes from an old crash */
    public boolean isOurBox(Block block) {
        if (boxes.containsKey(BlockKey.of(block))) return true;
        return block.getState(false) instanceof TileState tile && tile.getPersistentDataContainer().has(boxKey, PersistentDataType.STRING);
    }

    /** Can attacker hurt victim right now? Only called for two players. */
    public boolean canDamage(Player victim, Player attacker) {
        boolean victimIn = isParticipant(victim);
        boolean attackerIn = isParticipant(attacker);
        if (!victimIn && !attackerIn) return true;
        if (state != EventState.RUNNING) return false;
        if (victimIn && attackerIn) return config().isPvp();
        return false; // outsiders can't hit players in the event and vice versa
    }

    /**
     * Removes event boxes that are left over from a crash, found by their tag when the chunk loads.
     */
    public void checkChunkForOrphans(Chunk chunk) {
        List<Block> orphans = new ArrayList<>();
        for (BlockState blockState : chunk.getTileEntities(false)) {
            if (blockState instanceof TileState tile && tile.getPersistentDataContainer().has(boxKey, PersistentDataType.STRING)
                && !boxes.containsKey(BlockKey.of(blockState.getBlock()))) {
                orphans.add(blockState.getBlock());
            }
        }
        if (orphans.isEmpty()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Block block : orphans) {
                BlockKey key = BlockKey.of(block);
                if (boxes.containsKey(key)) continue;
                // startup recovery may already have put this block back
                if (!(block.getState(false) instanceof TileState tile) || !tile.getPersistentDataContainer().has(boxKey, PersistentDataType.STRING)) continue;
                String previous = store.boxes.remove(key);
                restoreBlock(key, previous);
                Utils.getDebugLogger("Removed a leftover warehouse box at " + key.serialize());
            }
            store.save();
        });
    }

    public List<Player> onlineParticipants() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : participants.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) players.add(player);
        }
        return players;
    }

    public List<String> getDebugLines() {
        List<String> lines = new ArrayList<>();
        long now = System.currentTimeMillis();
        lines.add("state: " + state);
        lines.add("next event in: " + Utils.formatTime((store.nextEventAt - now) / 1000) + (config().isAutoStart() ? "" : " (auto-start off)"));
        lines.add("queue open: " + queueManager.isOpen() + (queueManager.isOpen() ? " (closes in " + Utils.formatTime((queueManager.getClosesAt() - now) / 1000) + ")" : ""));
        List<String> queued = new ArrayList<>();
        for (UUID uuid : queueManager.getPlayersInQueue()) queued.add(nameOf(uuid));
        lines.add("queue (" + queued.size() + "): " + String.join(", ", queued));
        lines.add("warehouse: " + (activeWarehouse == null ? "none" : activeWarehouse.getName()));
        List<String> names = new ArrayList<>();
        for (UUID uuid : participants.keySet()) names.add(nameOf(uuid) + (frozen.contains(uuid) ? " (frozen)" : ""));
        lines.add("participants (" + names.size() + "): " + String.join(", ", names));
        lines.add("boxes tracked: " + boxes.size() + ", saved in state.yml: " + store.boxes.size());
        lines.add("pending returns: " + store.returns.size());
        if (state == EventState.RUNNING) lines.add("ends in: " + Utils.formatTime((endsAt - now) / 1000));
        return lines;
    }

    private String nameOf(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString() : name;
    }

    // =====================================================================================
    // helpers
    // =====================================================================================

    private void setState(EventState newState) {
        setStateNoSave(newState);
        store.save();
    }

    private void setStateNoSave(EventState newState) {
        Utils.getDebugLogger("state " + state + " -> " + newState);
        state = newState;
        store.state = newState;
    }

    private void hideQueueBar() {
        if (queueBar == null) return;
        BossBar bar = queueBar;
        queueBar = null;
        Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bar));
    }

    private void hideEventBar() {
        if (eventBar == null) return;
        BossBar bar = eventBar;
        eventBar = null;
        Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bar));
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
