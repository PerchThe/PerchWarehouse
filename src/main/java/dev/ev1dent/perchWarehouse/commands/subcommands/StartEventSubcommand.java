package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.MiniUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class StartEventSubcommand {

    private final WarehouseManager warehouseManager;
    private final MessageManager messageManager;


    public StartEventSubcommand(WarehousePlugin warehousePlugin){
        this.warehouseManager = new WarehouseManager();
        this.messageManager = new MessageManager(warehousePlugin);
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("startevent")
            .requires(source -> source.getSender().hasPermission("warehouse.startevent"))
            .then(Commands.argument("staging_time (minutes)", IntegerArgumentType.integer())
                .executes(ctx -> {
                    int stagingTime = ctx.getArgument("staging_time (minutes)", Integer.class);
                    warehouseManager.openRegistration(stagingTime);
                    ctx.getSource().getSender().sendMessage(MiniUtil.format(messageManager.getMessage("starting-event").replace("%time%", String.valueOf(stagingTime))));
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
}
