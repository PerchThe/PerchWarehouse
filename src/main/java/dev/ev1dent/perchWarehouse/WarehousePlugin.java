package dev.ev1dent.perchWarehouse;

import dev.ev1dent.perchWarehouse.commands.CommandWarehouse;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class WarehousePlugin extends JavaPlugin {

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onLoad() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
            event -> event.registrar().register(new CommandWarehouse().constructCommand(), "booster command", List.of(CommandWarehouse.CommandAliases))
        );
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
