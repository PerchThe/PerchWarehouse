package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.exceptions.QueueClosedException;
import dev.ev1dent.perchWarehouse.managers.EditorManager;
import dev.ev1dent.perchWarehouse.managers.QueueManager;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class JoinSubcommand {

    private final QueueManager queueManager;
    private final WarehouseManager warehouseManager;
    private final EditorManager editorManager;

    public JoinSubcommand(WarehousePlugin warehousePlugin) {
        this.queueManager = warehousePlugin.getQueueManager();
        this.warehouseManager = warehousePlugin.getWarehouseManager();
        this.editorManager = warehousePlugin.getEditorManager();
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("join")
            .requires(source -> source.getSender().hasPermission("warehouse.join"))
            .executes(ctx -> {
                if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                    ctx.getSource().getSender().sendMessage(Utils.format("players-only"));
                    return Command.SINGLE_SUCCESS;
                }
                if (editorManager.isEditing(player)) {
                    player.sendMessage(Utils.format("join-while-editing"));
                    return Command.SINGLE_SUCCESS;
                }
                if (warehouseManager.isParticipant(player)) {
                    player.sendMessage(Utils.format("already-in-event"));
                    return Command.SINGLE_SUCCESS;
                }
                try {
                    if (!queueManager.addPlayer(player)) {
                        player.sendMessage(Utils.format("already-queued"));
                        return Command.SINGLE_SUCCESS;
                    }
                } catch (QueueClosedException e) {
                    player.sendMessage(Utils.format(warehouseManager.getState().isInProgress() ? "event-in-progress" : "queue-closed"));
                    return Command.SINGLE_SUCCESS;
                }
                player.sendMessage(Utils.format("joined-warehouse", queueManager.getQueueSize()));
                return Command.SINGLE_SUCCESS;
            });
    }
}
