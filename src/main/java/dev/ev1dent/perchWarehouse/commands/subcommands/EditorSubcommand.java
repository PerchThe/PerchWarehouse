package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.data.Warehouse;
import dev.ev1dent.perchWarehouse.managers.EditorManager;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class EditorSubcommand {

    private final WarehousePlugin warehousePlugin;
    private final EditorManager editorManager;

    public EditorSubcommand(WarehousePlugin warehousePlugin){
        this.warehousePlugin = warehousePlugin;
        this.editorManager = warehousePlugin.getEditorManager();
    }

    /** /warehouse editor <name> to start placing markers, /warehouse editor exit to leave. */
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("editor")
            .requires(source -> source.getSender().hasPermission("warehouse.editor"))
            .then(Commands.literal("exit")
                .executes(ctx -> {
                    if (ctx.getSource().getExecutor() instanceof Player player && !editorManager.exit(player))
                        player.sendMessage(Utils.format("editor-not-editing"));
                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(warehouseSuggestions(warehousePlugin))
                .executes(ctx -> {
                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                        ctx.getSource().getSender().sendMessage(Utils.format("players-only"));
                        return Command.SINGLE_SUCCESS;
                    }
                    String name = ctx.getArgument("name", String.class);
                    Warehouse warehouse = warehousePlugin.getTierManager().getWarehouse(name);
                    if (warehouse == null) {
                        player.sendMessage(Utils.format("unknown-warehouse", name));
                        return Command.SINGLE_SUCCESS;
                    }
                    editorManager.enter(player, warehouse);
                    return Command.SINGLE_SUCCESS;
                }));
    }

    public static SuggestionProvider<CommandSourceStack> warehouseSuggestions(WarehousePlugin plugin) {
        return (ctx, builder) -> {
            String typed = builder.getRemainingLowerCase();
            for (Warehouse warehouse : plugin.getTierManager().getWarehouses()) {
                if (warehouse.getName().toLowerCase().startsWith(typed)) builder.suggest(warehouse.getName());
            }
            return builder.buildFuture();
        };
    }
}
