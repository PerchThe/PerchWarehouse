package dev.ev1dent.perchWarehouse.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ev1dent.perchWarehouse.managers.ConfigManager;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.MiniUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/*
/warehouse join - everyone (done)
/warehouse leave - everyone (done)

/warehouse add <name> - moderator (done)
/warehouse remove <name> - moderator (done)

/warehouse create <name> <player count> - admin
/warehouse edit <name> - admin
/warehouse reload - admin
 */
public class CommandWarehouse {

    QueueManager queueManager = new QueueManager();
    WarehouseManager warehouseManager = new WarehouseManager();
    ConfigManager configManager = new ConfigManager();

    public LiteralCommandNode<CommandSourceStack> constructCommand() {
        return Commands.literal("warehouse")

            // default commands
            .then(Commands.literal("join")
                .requires(source -> source.getSender().hasPermission("warehouse.join"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    queueManager.addPlayer((Player) ctx.getSource().getSender());
                    sender.sendMessage(MiniUtil.format("<green>Joined warehouse!"));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("leave")
                .requires(source -> source.getSender().hasPermission("warehouse.leave"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    queueManager.removePlayer((Player) ctx.getSource().getSender());
                    sender.sendMessage(MiniUtil.format("<green>Left warehouse!"));
                    return Command.SINGLE_SUCCESS;
                })
            )

            //moderator commands
            .then(Commands.literal("add")
                .requires(source -> source.getSender().hasPermission("warehouse.add"))
                .then(Commands.argument("player", ArgumentTypes.player())
                    .executes(ctx -> {
                        queueManager.addPlayer(ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                        CommandSender sender = ctx.getSource().getSender();
                        sender.sendMessage(MiniUtil.format("<green>Added player to warehouse queue"));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("remove")
                .requires(source -> source.getSender().hasPermission("warehouse.remove"))
                .then(Commands.argument("player", ArgumentTypes.player())
                    .executes(ctx -> {
                        queueManager.removePlayer(ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                        CommandSender sender = ctx.getSource().getSender();
                        sender.sendMessage(MiniUtil.format("<green>Removed player from warehouse queue"));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )

            //administrator commands
            .then(Commands.literal("create")
                .requires(source -> source.getSender().hasPermission("warehouse.create"))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> {
                        warehouseManager.create(ctx.getArgument("name", String.class));
                        CommandSender sender = ctx.getSource().getSender();
                        sender.sendMessage(MiniUtil.format("<green>Created warehouse..."));
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
                        sender.sendMessage(MiniUtil.format("<green>Deleted warehouse..."));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("edit")
                .requires(source -> source.getSender().hasPermission("warehouse.edit"))
                .then(Commands.argument("player", ArgumentTypes.player())
                    .executes(ctx -> {
                        warehouseManager.edit(ctx.getArgument("name", String.class));
                        CommandSender sender = ctx.getSource().getSender();
                        sender.sendMessage(MiniUtil.format("<green>Opening warehouse editor..."));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("warehouse.reload"))
                .executes(ctx -> {
                    configManager.reload();
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(MiniUtil.format("<green>Configuration reloaded..."));
                    return Command.SINGLE_SUCCESS;
                })
            )

            .build();
    }
}
