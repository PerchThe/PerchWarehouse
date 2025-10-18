package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.MiniUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class CreateSubcommand {

    private final WarehouseManager warehouseManager;
    private final MessageManager messageManager;


    public CreateSubcommand(WarehousePlugin warehousePlugin){
        this.warehouseManager = new WarehouseManager();
        this.messageManager = new MessageManager(warehousePlugin);
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("create")
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
            );
    }
}