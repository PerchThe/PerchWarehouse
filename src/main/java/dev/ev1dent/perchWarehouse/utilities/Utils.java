package dev.ev1dent.perchWarehouse.utilities;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {

    private static WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }

    public static Component miniMessage(String s){
        return MiniMessage.miniMessage().deserialize(s == null ? "" : s);
    }

    /**
     * MiniMessage for item names / lore. Minecraft makes custom names italic by default,
     * so turn that off unless the config explicitly asks for <italic>.
     */
    public static Component itemText(String s){
        return miniMessage(s).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static void getDebugLogger(String s){
        if(!warehousePlugin().getConfigManager().getBoolean("debug")) return;
        warehousePlugin().getLogger().info("[DEBUG] " + s);
    }

    public static void getInfoLogger(String s){
        warehousePlugin().getLogger().info(s);
    }

    public static void getWarnLogger(String s){
        warehousePlugin().getLogger().warning(s);
    }

    public static String parsePlaceholders(String message, Object... args) {
        String msg = warehousePlugin().getMessageManager().getMessage(message);
        if (msg == null) msg = "<red>Missing message: " + message;
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return msg;
    }

    public static Component format(String key, Object... args){
        String parsed = parsePlaceholders(key, args);
        return miniMessage(parsed);
    }

    /** Formats seconds as m:ss */
    public static String formatTime(long seconds){
        if (seconds < 0) seconds = 0;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Parses "5" or "1-3" into a random number in that range (inclusive).
     */
    public static int rollRange(int[] range){
        if (range[0] >= range[1]) return range[0];
        return ThreadLocalRandom.current().nextInt(range[0], range[1] + 1);
    }

    /**
     * Parses "5" or "1-3" into {min, max}. throws IllegalArgumentException when it isn't a valid range.
     */
    public static int[] parseRange(String input){
        if (input == null) throw new IllegalArgumentException("missing value");
        String s = input.trim();
        try {
            if (s.contains("-")) {
                String[] split = s.split("-", 2);
                int min = Integer.parseInt(split[0].trim());
                int max = Integer.parseInt(split[1].trim());
                if (min < 0 || max < min) throw new IllegalArgumentException("'" + input + "' is not a valid range");
                return new int[]{min, max};
            }
            int value = Integer.parseInt(s);
            if (value < 0) throw new IllegalArgumentException("'" + input + "' can't be negative");
            return new int[]{value, value};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + input + "' is not a number or range like 1-3");
        }
    }

    /**
     * Parses a bukkit color name (RED, AQUA...) or a hex color (#RRGGBB).
     */
    public static Color parseColor(String input){
        if (input == null) throw new IllegalArgumentException("missing color");
        String s = input.trim();
        if (s.startsWith("#") && s.length() == 7) {
            try {
                return Color.fromRGB(Integer.parseInt(s.substring(1), 16));
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("'" + input + "' is not a valid hex color");
            }
        }
        try {
            Field field = Color.class.getField(s.toUpperCase(Locale.ROOT));
            if (field.getType() == Color.class) return (Color) field.get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        throw new IllegalArgumentException("'" + input + "' is not a color name or #RRGGBB");
    }

    /** Sends a message key to a player only if they are online. */
    public static void send(Player player, String key, Object... args){
        if (player != null && player.isOnline()) player.sendMessage(format(key, args));
    }
}
