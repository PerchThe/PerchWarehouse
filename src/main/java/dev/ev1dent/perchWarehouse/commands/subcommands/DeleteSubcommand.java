package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.data.Warehouse;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class DeleteSubcommand {

    private final WarehousePlugin warehousePlugin;

    public DeleteSubcommand(WarehousePlugin warehousePlugin){
        this.warehousePlugin = warehousePlugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("delete")
            .requires(source -> source.getSender().hasPermission("warehouse.delete"))
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(EditorSubcommand.warehouseSuggestions(warehousePlugin))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    String name = ctx.getArgument("name", String.class);
                    Warehouse active = warehousePlugin.getWarehouseManager().getActiveWarehouse();
                    if ((active != null && active.getName().equals(name)) || warehousePlugin.getEditorManager().isBeingEdited(name)) {
                        sender.sendMessage(Utils.format("warehouse-in-use", name));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (!warehousePlugin.getTierManager().delete(name)) {
                        sender.sendMessage(Utils.format("unknown-warehouse", name));
                        return Command.SINGLE_SUCCESS;
                    }
                    Utils.getDebugLogger("deleted warehouse " + name);
                    sender.sendMessage(Utils.format("deleted-warehouse", name));
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
}
