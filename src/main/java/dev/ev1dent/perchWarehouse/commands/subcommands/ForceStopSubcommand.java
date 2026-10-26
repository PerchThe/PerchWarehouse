package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class ForceStopSubcommand {

    private final WarehouseManager warehouseManager;

    public ForceStopSubcommand(WarehousePlugin warehousePlugin){
        this.warehouseManager = warehousePlugin.getWarehouseManager();
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("forcestop")
            .requires(source -> source.getSender().hasPermission("warehouse.forcestop"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                sender.sendMessage(Utils.format(warehouseManager.stop() ? "stopped-warehouse" : "nothing-to-stop"));
                return Command.SINGLE_SUCCESS;
            });
    }
}
