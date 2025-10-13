package dev.ev1dent.perchWarehouse;

import dev.ev1dent.perchWarehouse.commands.CommandWarehouse;
import dev.ev1dent.perchWarehouse.configuration.ConfigManager;
import dev.ev1dent.perchWarehouse.listeners.PlayerQuitListener;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

public final class WarehousePlugin extends JavaPlugin {

    QueueManager queueManager = new QueueManager();
    public QueueManager getQueueManager() {
        return queueManager;
    }
    ConfigManager configManager = new ConfigManager(this);

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onLoad() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
            event -> event.registrar().register(new CommandWarehouse().constructCommand(), "booster command", (Collection<String>) configManager.getList("aliases"))
        );
    }

    @Override
    public void onEnable() {
        queueManager.isOpened = false;
        registerEvents();
    }

    @Override
    public void onDisable() {

    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
    }
}
