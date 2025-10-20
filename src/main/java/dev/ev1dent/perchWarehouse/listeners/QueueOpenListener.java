package dev.ev1dent.perchWarehouse.listeners;

import dev.ev1dent.perchWarehouse.Events.QueueOpenEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QueueOpenListener implements Listener {
    @EventHandler
    public void onQueueOpen(QueueOpenEvent event) {
        System.out.println("QueueOpenListener has fired");
    }
}
