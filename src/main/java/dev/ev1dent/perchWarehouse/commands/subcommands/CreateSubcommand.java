package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.TierManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class CreateSubcommand {

    private final WarehousePlugin warehousePlugin;

    public CreateSubcommand(WarehousePlugin warehousePlugin){
        this.warehousePlugin = warehousePlugin;
    }

    private TierManager tierManager() {
        return warehousePlugin.getTierManager();
    }

    /** Creates warehouses/<name>.yml with starter crates, in the world you're standing in. */
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("create")
            .requires(source -> source.getSender().hasPermission("warehouse.create"))
            .then(Commands.argument("name", StringArgumentType.word())
                .then(Commands.argument("capacity", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        String name = ctx.getArgument("name", String.class);
                        int capacity = ctx.getArgument("capacity", Integer.class);
                        Entity executor = ctx.getSource().getExecutor();
                        String world = executor != null ? executor.getWorld().getName() : Bukkit.getWorlds().getFirst().getName();
                        if (tierManager().create(name, capacity, world) == null) {
                            sender.sendMessage(Utils.format("warehouse-exists", name));
                            return Command.SINGLE_SUCCESS;
                        }
                        Utils.getDebugLogger("created warehouse " + name + " " + capacity);
                        sender.sendMessage(Utils.format("created-warehouse", name, capacity));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            );
    }
}
