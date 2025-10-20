package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class ForceStartSubcommand {

    private final WarehouseManager warehouseManager;


    public ForceStartSubcommand(WarehousePlugin warehousePlugin){
        this.warehouseManager = new WarehouseManager();
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("forcestart")
            .requires(source -> source.getSender().hasPermission("warehouse.forcestart"))
            .executes(ctx -> {
                warehouseManager.start();
                CommandSender sender = ctx.getSource().getSender();
                sender.sendMessage(Utils.format("started-warehouse"));
                return Command.SINGLE_SUCCESS;
            });
    }
}
