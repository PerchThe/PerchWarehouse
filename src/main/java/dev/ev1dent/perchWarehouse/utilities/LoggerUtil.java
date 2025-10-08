package dev.ev1dent.perchWarehouse.utilities;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.configuration.ConfigManager;

public class LoggerUtil {

    private static WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    static ConfigManager config = new ConfigManager(warehousePlugin());

    public static void debug(String s){
        if(!config.getBoolean("debug")) return;
        warehousePlugin().getLogger().info("[DEBUG] " + s);
    }

    public static void info(String s){
        warehousePlugin().getLogger().info(s);
    }
}
