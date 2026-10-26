package dev.ev1dent.perchWarehouse.Events;

import dev.ev1dent.perchWarehouse.data.Warehouse;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

/**
 * Called when the countdown finishes and the boxes are placed.
 */
public class WarehouseStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Warehouse warehouse;
    private final List<Player> players;

    public WarehouseStartEvent(Warehouse warehouse, List<Player> players){
        this.warehouse = warehouse;
        this.players = players;
    }

    public Warehouse getWarehouse(){
        return warehouse;
    }

    public List<Player> getPlayers(){
        return players;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
