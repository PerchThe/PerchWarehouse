package dev.ev1dent.perchWarehouse.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import dev.ev1dent.perchWarehouse.exceptions.QueueClosedException;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import dev.ev1dent.perchWarehouse.configuration.TierManager;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.MiniUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandWarehouse {

    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    MessageManager messageManager = new MessageManager(warehousePlugin());
    WarehouseManager warehouseManager = new WarehouseManager();
    QueueManager queueManager = warehousePlugin().getQueueManager();
    TierManager tierManager = new TierManager(warehousePlugin());

    public LiteralCommandNode<CommandSourceStack> constructCommand() {
        return Commands.literal("warehouse")

            // default commands
            .then(Commands.literal("join")
                .requires(source -> source.getSender().hasPermission("warehouse.join"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    try {
                        queueManager.addPlayer((Player) ctx.getSource().getSender());
                    } catch (QueueClosedException e) {
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(MiniUtil.format(messageManager.getMessage("joined-warehouse")));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("leave")
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
                })
            )

            //moderator commands
            .then(Commands.literal("add")
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
                )
            )
            .then(Commands.literal("kick")
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
                        sender.sendMessage(MiniUtil.format(messageManager.getMessage("kicked-from-warehouse").replace("%player%", player.getName())));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )

            //administrator commands
            .then(Commands.literal("startevent")
                .requires(source -> source.getSender().hasPermission("warehouse.startevent"))
                    .then(Commands.argument("staging_time (minutes)", IntegerArgumentType.integer())
                    .executes(ctx -> {
                        int stagingTime = ctx.getArgument("staging_time (minutes)", Integer.class);
                        warehouseManager.openRegistration(stagingTime);
                        ctx.getSource().getSender().sendMessage(MiniUtil.format(messageManager.getMessage("starting-event").replace("%time%", String.valueOf(stagingTime))));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("create")
                .requires(source -> source.getSender().hasPermission("warehouse.create"))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("capacity", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            warehouseManager.create(ctx.getArgument("name", String.class), ctx.getArgument("capacity", Integer.class));
                            CommandSender sender = ctx.getSource().getSender();
                            sender.sendMessage(MiniUtil.format(messageManager.getMessage("created-warehouse")));
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .then(Commands.literal("edit")
                .requires(source -> source.getSender().hasPermission("warehouse.edit"))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> {
                        warehouseManager.edit(ctx.getArgument("name", String.class));
                        CommandSender sender = ctx.getSource().getSender();
                        sender.sendMessage(MiniUtil.format(messageManager.getMessage("editing-warehouse")));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("delete")
                .requires(source -> source.getSender().hasPermission("warehouse.delete"))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> {
                        warehouseManager.delete(ctx.getArgument("name", String.class));
                        CommandSender sender = ctx.getSource().getSender();
                        sender.sendMessage(MiniUtil.format(messageManager.getMessage("deleted-warehouse")));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("forcestart")
                .requires(source -> source.getSender().hasPermission("warehouse.forcestart"))
                .executes(ctx -> {
                    warehouseManager.start();
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(MiniUtil.format(messageManager.getMessage("started-warehouse")));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("forcestop")
                .requires(source -> source.getSender().hasPermission("warehouse.forcestop"))
                .executes(ctx -> {
                    warehouseManager.stop();
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(MiniUtil.format(messageManager.getMessage("stopped-warehouse")));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("warehouse.reload"))
                .executes(ctx -> {
                    messageManager.reloadConfig();
                    tierManager.reload();
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(MiniUtil.format(messageManager.getMessage("reloaded-configuration")));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
