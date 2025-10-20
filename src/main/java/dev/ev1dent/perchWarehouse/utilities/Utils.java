package dev.ev1dent.perchWarehouse.utilities;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.ConfigManager;
import dev.ev1dent.perchWarehouse.configuration.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Utils {

    private static WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    static ConfigManager config = new ConfigManager(warehousePlugin());
    static MessageManager messageManager = new MessageManager(warehousePlugin());

    public static Component miniMessage(String s){
        return MiniMessage.miniMessage().deserialize(s);
    }

    public static void getDebugLogger(String s){
        if(!config.getBoolean("debug")) return;
        warehousePlugin().getLogger().info("[DEBUG] " + s);
    }

    public static void getInfoLogger(String s){
        warehousePlugin().getLogger().info(s);
    }

    public static String parsePlaceholders(String message, Object... args) {
        String msg = messageManager.getMessage(message);
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return msg;
    }

    public static Component format(String key, Object... args){
        String parsed = parsePlaceholders(key, args);
        return miniMessage(parsed);
    }

}
