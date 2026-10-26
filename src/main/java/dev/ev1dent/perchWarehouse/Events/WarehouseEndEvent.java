package dev.ev1dent.perchWarehouse.Events;

import dev.ev1dent.perchWarehouse.data.Warehouse;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called after an event (or an open queue) has been cleaned up. warehouse is null if the event never got that far.
 */
public class WarehouseEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Warehouse warehouse;
    private final String reason;

    public WarehouseEndEvent(Warehouse warehouse, String reason){
        this.warehouse = warehouse;
        this.reason = reason;
    }

    public Warehouse getWarehouse(){
        return warehouse;
    }

    public String getReason(){
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
