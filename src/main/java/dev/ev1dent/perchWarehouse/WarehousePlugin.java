package dev.ev1dent.perchWarehouse;

import dev.ev1dent.perchWarehouse.commands.CommandWarehouse;
import dev.ev1dent.perchWarehouse.configuration.ConfigManager;
import dev.ev1dent.perchWarehouse.configuration.LootManager;
import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import dev.ev1dent.perchWarehouse.configuration.TierManager;
import dev.ev1dent.perchWarehouse.listeners.BoxListener;
import dev.ev1dent.perchWarehouse.listeners.ChunkListener;
import dev.ev1dent.perchWarehouse.listeners.CombatListener;
import dev.ev1dent.perchWarehouse.listeners.EditorListener;
import dev.ev1dent.perchWarehouse.listeners.FreezeListener;
import dev.ev1dent.perchWarehouse.listeners.PlayerJoinListener;
import dev.ev1dent.perchWarehouse.listeners.PlayerQuitListener;
import dev.ev1dent.perchWarehouse.listeners.QueueOpenListener;
import dev.ev1dent.perchWarehouse.managers.EditorManager;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;

public final class WarehousePlugin extends JavaPlugin {

    // one instance of each, shared by everything (get them through the plugin)
    private ConfigManager configManager;
    private MessageManager messageManager;
    private TierManager tierManager;
    private LootManager lootManager;
    private QueueManager queueManager;
    private WarehouseManager warehouseManager;
    private EditorManager editorManager;

    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public TierManager getTierManager() { return tierManager; }
    public LootManager getLootManager() { return lootManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public WarehouseManager getWarehouseManager() { return warehouseManager; }
    public EditorManager getEditorManager() { return editorManager; }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.reloadConfig();
        messageManager = new MessageManager(this);
        tierManager = new TierManager(this);
        lootManager = new LootManager(this);
        tierManager.validateRarities(lootManager);
        queueManager = new QueueManager(this);
        warehouseManager = new WarehouseManager(this);
        editorManager = new EditorManager(this);

        Collection<String> aliases = new ArrayList<>(configManager.getStringList("aliases"));
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
            event -> event.registrar().register(new CommandWarehouse().constructCommand(), "Warehouse event command", aliases)
        );

        registerEvents();
        warehouseManager.enable();
    }

    @Override
    public void onDisable() {
        if (editorManager != null) editorManager.exitAll();
        if (warehouseManager != null) warehouseManager.disable();
    }

    public void reloadAll() {
        configManager.reloadConfig();
        messageManager.reloadConfig();
        tierManager.reload();
        lootManager.reload();
        tierManager.validateRarities(lootManager);
        editorManager.rebind();
    }

    private void registerEvents() {
        var pm = this.getServer().getPluginManager();
        pm.registerEvents(new PlayerQuitListener(), this);
        pm.registerEvents(new PlayerJoinListener(), this);
        pm.registerEvents(new FreezeListener(), this);
        pm.registerEvents(new BoxListener(), this);
        pm.registerEvents(new CombatListener(), this);
        pm.registerEvents(new EditorListener(), this);
        pm.registerEvents(new ChunkListener(), this);
        pm.registerEvents(new QueueOpenListener(), this);
    }
}
