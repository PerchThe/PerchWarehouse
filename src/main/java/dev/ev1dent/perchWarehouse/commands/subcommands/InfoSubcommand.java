package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class InfoSubcommand {

    private final WarehouseManager warehouseManager;
    private final QueueManager queueManager;
    private final WarehousePlugin warehousePlugin;

    public InfoSubcommand(WarehousePlugin warehousePlugin){
        this.warehousePlugin = warehousePlugin;
        this.warehouseManager = warehousePlugin.getWarehouseManager();
        this.queueManager = warehousePlugin.getQueueManager();
    }

    /** Tells players when the next event is, or what's happening right now. */
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("info")
            .requires(source -> source.getSender().hasPermission("warehouse.info"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                long now = System.currentTimeMillis();
                switch (warehouseManager.getState()) {
                    case IDLE -> {
                        if (warehousePlugin.getConfigManager().isAutoStart())
                            sender.sendMessage(Utils.format("info-next-event", Utils.formatTime((warehouseManager.getNextEventAt() - now) / 1000)));
                        else
                            sender.sendMessage(Utils.format("info-no-schedule"));
                    }
                    case QUEUE_OPEN -> sender.sendMessage(Utils.format("info-queue-open",
                        Utils.formatTime((queueManager.getClosesAt() - now) / 1000), queueManager.getQueueSize()));
                    case RUNNING -> sender.sendMessage(Utils.format("info-running",
                        Utils.formatTime((warehouseManager.getEndsAt() - now) / 1000), warehouseManager.getBoxesLeft()));
                    default -> sender.sendMessage(Utils.format("info-starting"));
                }
                return Command.SINGLE_SUCCESS;
            });
    }
}
