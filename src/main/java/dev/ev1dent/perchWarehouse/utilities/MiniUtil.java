package dev.ev1dent.perchWarehouse.utilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MiniUtil {
    public static Component format (String s){
        return MiniMessage.miniMessage().deserialize(s);
    }
}
