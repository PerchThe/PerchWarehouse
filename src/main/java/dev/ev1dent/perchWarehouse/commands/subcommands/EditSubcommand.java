package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.managers.WarehouseManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class EditSubcommand {

    private final WarehouseManager warehouseManager;

    public EditSubcommand(){
        this.warehouseManager = new WarehouseManager();
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("edit")
            .requires(source -> source.getSender().hasPermission("warehouse.edit"))
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> {
                    warehouseManager.edit(ctx.getArgument("name", String.class));
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(Utils.format("editing-warehouse"));
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
}