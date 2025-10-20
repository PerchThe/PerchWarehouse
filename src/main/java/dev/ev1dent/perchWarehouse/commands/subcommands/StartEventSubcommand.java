package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class StartEventSubcommand {

    private final WarehouseManager warehouseManager;

    public StartEventSubcommand(WarehousePlugin warehousePlugin){
        this.warehouseManager = new WarehouseManager();
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("startevent")
            .requires(source -> source.getSender().hasPermission("warehouse.startevent"))
            .then(Commands.argument("staging_time (minutes)", IntegerArgumentType.integer())
                .executes(ctx -> {
                    int stagingTime = ctx.getArgument("staging_time (minutes)", Integer.class);
                    warehouseManager.openRegistration(stagingTime);
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(Utils.format("starting-event", String.valueOf(stagingTime)));
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
}
