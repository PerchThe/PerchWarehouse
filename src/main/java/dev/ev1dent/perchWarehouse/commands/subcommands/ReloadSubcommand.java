package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class ReloadSubcommand {

    private final WarehousePlugin warehousePlugin;

    public ReloadSubcommand(WarehousePlugin warehousePlugin){
        this.warehousePlugin = warehousePlugin;
    }

    /** Reloads every yml. A running event keeps the warehouse and loot it started with. */
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("reload")
            .requires(source -> source.getSender().hasPermission("warehouse.reload"))
            .executes(ctx -> {
                warehousePlugin.reloadAll();
                CommandSender sender = ctx.getSource().getSender();
                sender.sendMessage(Utils.format("reloaded-configuration"));
                return Command.SINGLE_SUCCESS;
            });
    }
}
