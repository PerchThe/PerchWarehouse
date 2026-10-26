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
    private final WarehousePlugin warehousePlugin;

    public StartEventSubcommand(WarehousePlugin warehousePlugin){
        this.warehousePlugin = warehousePlugin;
        this.warehouseManager = warehousePlugin.getWarehouseManager();
    }

    /** Opens the queue now. With no argument it uses event.queue-open-seconds from config.yml. */
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("startevent")
            .requires(source -> source.getSender().hasPermission("warehouse.startevent"))
            .executes(ctx -> open(ctx.getSource().getSender(), warehousePlugin.getConfigManager().getQueueOpenSeconds()))
            .then(Commands.argument("staging_time (minutes)", IntegerArgumentType.integer(1, 120))
                .executes(ctx -> open(ctx.getSource().getSender(), ctx.getArgument("staging_time (minutes)", Integer.class) * 60))
            );
    }

    private int open(CommandSender sender, int seconds) {
        String problem = warehouseManager.openRegistration(seconds);
        if (problem != null) sender.sendMessage(Utils.format(problem));
        else sender.sendMessage(Utils.format("starting-event", Utils.formatTime(seconds)));
        return Command.SINGLE_SUCCESS;
    }
}
