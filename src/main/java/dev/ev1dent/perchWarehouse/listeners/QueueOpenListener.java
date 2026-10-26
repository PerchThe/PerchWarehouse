package dev.ev1dent.perchWarehouse.listeners;

import dev.ev1dent.perchWarehouse.Events.QueueOpenEvent;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QueueOpenListener implements Listener {
    @EventHandler
    public void onQueueOpen(QueueOpenEvent event) {
        Utils.getDebugLogger("QueueOpenEvent fired, queue open for " + event.getTime() + " seconds");
    }
}
