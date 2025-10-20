package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class ForceStopSubcommand {

    private final WarehouseManager warehouseManager;


    public ForceStopSubcommand(){
        this.warehouseManager = new WarehouseManager();
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("forcestop")
            .requires(source -> source.getSender().hasPermission("warehouse.forcestop"))
            .executes(ctx -> {
                warehouseManager.stop();
                CommandSender sender = ctx.getSource().getSender();
                sender.sendMessage(Utils.format("stopped-warehouse"));
                return Command.SINGLE_SUCCESS;
            });
    }
}

