package dev.ev1dent.perchWarehouse.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import dev.ev1dent.perchWarehouse.configuration.TierManager;
import dev.ev1dent.perchWarehouse.utilities.MiniUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

public class ReloadSubcommand {

    private final TierManager tierManager;
    private final MessageManager messageManager;


    public ReloadSubcommand(WarehousePlugin warehousePlugin){
        this.tierManager = new TierManager(warehousePlugin);
        this.messageManager = new MessageManager(warehousePlugin);
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("reload")
            .requires(source -> source.getSender().hasPermission("warehouse.reload"))
            .executes(ctx -> {
                messageManager.reloadConfig();
                tierManager.reload();
                CommandSender sender = ctx.getSource().getSender();
                sender.sendMessage(MiniUtil.format(messageManager.getMessage("reloaded-configuration")));
                return Command.SINGLE_SUCCESS;
            });
    }
}