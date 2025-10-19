package dev.ev1dent.perchWarehouse.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.commands.subcommands.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class CommandWarehouse {

    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    public LiteralCommandNode<CommandSourceStack> constructCommand() {
        return Commands.literal("warehouse")

            //default commands
            .then(new JoinSubcommand(warehousePlugin()).create())
            .then(new LeaveSubcommand(warehousePlugin()).create())

            //moderator commands
            .then(new AddSubcommand(warehousePlugin()).create())
            .then(new KickSubcommand(warehousePlugin()).create())

            //administrator commands
            .then(new StartEventSubcommand(warehousePlugin()).create())
            .then(new CreateSubcommand(warehousePlugin()).create())
            .then(new EditSubcommand(warehousePlugin()).create())
            .then(new DeleteSubcommand(warehousePlugin()).create())
            .then(new ForceStartSubcommand(warehousePlugin()).create())
            .then(new ForceStopSubcommand(warehousePlugin()).create())
            .then(new ReloadSubcommand(warehousePlugin()).create())
            .build();
    }
}
