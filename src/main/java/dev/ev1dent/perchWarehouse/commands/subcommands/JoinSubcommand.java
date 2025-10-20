package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.exceptions.QueueClosedException;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinSubcommand {

    private final QueueManager queueManager;

    public JoinSubcommand(WarehousePlugin warehousePlugin) {
        this.queueManager = warehousePlugin.getQueueManager();
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("join")
            .requires(source -> source.getSender().hasPermission("warehouse.join"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                try {
                    queueManager.addPlayer((Player) ctx.getSource().getSender());
                } catch (QueueClosedException e) {
                    return Command.SINGLE_SUCCESS;
                }
                sender.sendMessage(Utils.format("joined-warehouse"));
                return Command.SINGLE_SUCCESS;
            });
    }
}
