package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.MiniUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class ForceStartSubcommand {

    private final WarehouseManager warehouseManager;
    private final MessageManager messageManager;


    public ForceStartSubcommand(WarehousePlugin warehousePlugin){
        this.warehouseManager = new WarehouseManager();
        this.messageManager = new MessageManager(warehousePlugin);
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("forcestart")
            .requires(source -> source.getSender().hasPermission("warehouse.forcestart"))
            .executes(ctx -> {
                warehouseManager.start();
                CommandSender sender = ctx.getSource().getSender();
                sender.sendMessage(MiniUtil.format(messageManager.getMessage("started-warehouse")));
                return Command.SINGLE_SUCCESS;
            });
    }
}
