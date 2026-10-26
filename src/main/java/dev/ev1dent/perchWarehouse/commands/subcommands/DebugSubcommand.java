package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.data.MarkerType;
import dev.ev1dent.perchWarehouse.data.Warehouse;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class DebugSubcommand {

    private final WarehousePlugin warehousePlugin;

    public DebugSubcommand(WarehousePlugin warehousePlugin){
        this.warehousePlugin = warehousePlugin;
    }

    /** Dumps the current state, queue, participants, tracked boxes and every warehouse's setup. */
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("debug")
            .requires(source -> source.getSender().hasPermission("warehouse.debug"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                sender.sendMessage(Utils.miniMessage("<gold><bold>Warehouse debug"));
                for (String line : warehousePlugin.getWarehouseManager().getDebugLines()) {
                    sender.sendMessage(Utils.miniMessage("<gray>" + line));
                }
                for (Warehouse warehouse : warehousePlugin.getTierManager().getWarehouses()) {
                    String problem = warehouse.getProblem();
                    sender.sendMessage(Utils.miniMessage("<yellow>" + warehouse.getName() + "<gray> (capacity " + warehouse.getCount()
                        + ", world " + warehouse.getWorldName() + "): "
                        + warehouse.getMarkers(MarkerType.PLAYER_SPAWN).size() + " spawns, "
                        + warehouse.getMarkers(MarkerType.GUARANTEED).size() + " guaranteed spots, "
                        + warehouse.getMarkers(MarkerType.RANDOM).size() + " random spots (" + warehouse.getRandomSpawns() + " used) - "
                        + (problem == null ? "<green>ready" : "<red>" + problem)));
                    StringBuilder crates = new StringBuilder();
                    for (Warehouse.CrateType crate : warehouse.getCrateTypes()) {
                        if (!crates.isEmpty()) crates.append(", ");
                        boolean exists = warehousePlugin.getLootManager().getRarity(crate.rarity()) != null;
                        crates.append(exists ? "" : "<red>").append(crate.rarity()).append(" w").append(crate.weight());
                        if (crate.minimum() > 0) crates.append(" min").append(crate.minimum());
                        if (crate.hasMaximum()) crates.append(" max").append(crate.maximum());
                        crates.append(exists ? "" : " (no loot file)</red>");
                    }
                    sender.sendMessage(Utils.miniMessage("<gray>   crates: " + (crates.isEmpty() ? "<red>none" : crates.toString())));
                }
                return Command.SINGLE_SUCCESS;
            });
    }
}
