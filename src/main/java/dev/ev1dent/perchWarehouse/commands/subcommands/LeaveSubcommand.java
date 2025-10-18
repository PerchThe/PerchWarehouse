package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import dev.ev1dent.perchWarehouse.exceptions.QueueClosedException;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import dev.ev1dent.perchWarehouse.utilities.MiniUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveSubcommand {

    private final QueueManager queueManager;
    private final MessageManager messageManager;


    public LeaveSubcommand(WarehousePlugin warehousePlugin){
        this.queueManager = warehousePlugin.getQueueManager();
        this.messageManager = new MessageManager(warehousePlugin);
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
       return Commands.literal("leave")
            .requires(source -> source.getSender().hasPermission("warehouse.leave"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                try {
                    queueManager.removePlayer((Player) ctx.getSource().getSender());
                } catch (QueueClosedException e) {
                    return Command.SINGLE_SUCCESS;
                }
                sender.sendMessage(MiniUtil.format(messageManager.getMessage("left-warehouse")));
                return Command.SINGLE_SUCCESS;
            });
    }
}
