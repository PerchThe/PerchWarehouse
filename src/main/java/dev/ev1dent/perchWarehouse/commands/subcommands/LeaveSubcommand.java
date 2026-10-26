package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.exceptions.QueueClosedException;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class LeaveSubcommand {

    private final QueueManager queueManager;
    private final WarehouseManager warehouseManager;

    public LeaveSubcommand(WarehousePlugin warehousePlugin){
        this.queueManager = warehousePlugin.getQueueManager();
        this.warehouseManager = warehousePlugin.getWarehouseManager();
    }

    /** Leaves the queue, or the event itself if you're already inside. */
    public LiteralArgumentBuilder<CommandSourceStack> create() {
       return Commands.literal("leave")
            .requires(source -> source.getSender().hasPermission("warehouse.leave"))
            .executes(ctx -> {
                if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                    ctx.getSource().getSender().sendMessage(Utils.format("players-only"));
                    return Command.SINGLE_SUCCESS;
                }
                if (warehouseManager.leaveEvent(player)) {
                    player.sendMessage(Utils.format("left-event"));
                    return Command.SINGLE_SUCCESS;
                }
                try {
                    if (!queueManager.removePlayer(player)) {
                        player.sendMessage(Utils.format("not-queued"));
                        return Command.SINGLE_SUCCESS;
                    }
                } catch (QueueClosedException e) {
                    player.sendMessage(Utils.format("queue-closed"));
                    return Command.SINGLE_SUCCESS;
                }
                player.sendMessage(Utils.format("left-warehouse"));
                return Command.SINGLE_SUCCESS;
            });
    }
}
