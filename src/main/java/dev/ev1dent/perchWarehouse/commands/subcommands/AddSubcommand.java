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
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddSubcommand {

    private final QueueManager queueManager;
    private final MessageManager messageManager;


    public AddSubcommand(WarehousePlugin warehousePlugin){
        this.queueManager = warehousePlugin.getQueueManager();
        this.messageManager = new MessageManager(warehousePlugin);
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("add")
            .requires(source -> source.getSender().hasPermission("warehouse.add"))
            .then(Commands.argument("player", ArgumentTypes.player())
                .executes(ctx -> {
                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
                    try {
                        queueManager.addPlayer(player);
                    } catch (QueueClosedException e) {
                        return Command.SINGLE_SUCCESS;
                    }
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(MiniUtil.format(messageManager.getMessage("added-to-warehouse").replace("%player%", player.getName())));
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
}
