package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.exceptions.QueueClosedException;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickSubcommand {

    private final QueueManager queueManager;

    public KickSubcommand(WarehousePlugin warehousePlugin){
        this.queueManager = warehousePlugin.getQueueManager();
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("kick")
            .requires(source -> source.getSender().hasPermission("warehouse.kick"))
            .then(Commands.argument("player", ArgumentTypes.player())
                .executes(ctx -> {
                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
                    try {
                        queueManager.removePlayer((Player) ctx.getSource().getSender());
                    } catch (QueueClosedException e) {
                        return Command.SINGLE_SUCCESS;
                    }
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(Utils.format("kicked-from-warehouse", player.getName()));
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
}