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

public class AddSubcommand {

    private final QueueManager queueManager;

    public AddSubcommand(WarehousePlugin warehousePlugin){
        this.queueManager = warehousePlugin.getQueueManager();
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("add")
            .requires(source -> source.getSender().hasPermission("warehouse.add"))
            .then(Commands.argument("player", ArgumentTypes.player())
                .executes(ctx -> {
                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
                    CommandSender sender = ctx.getSource().getSender();
                    try {
                        if (!queueManager.addPlayer(player)) {
                            sender.sendMessage(Utils.format("already-queued-other", player.getName()));
                            return Command.SINGLE_SUCCESS;
                        }
                    } catch (QueueClosedException e) {
                        sender.sendMessage(Utils.format("queue-closed"));
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(Utils.format("added-to-warehouse", player.getName()));
                    player.sendMessage(Utils.format("added-by-staff"));
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
}
