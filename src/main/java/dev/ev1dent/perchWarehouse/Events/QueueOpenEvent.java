package dev.ev1dent.perchWarehouse.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QueueOpenEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final int time;

    private QueueOpenEvent(int time){
        this.time = time;
    }

    public int getTime(){
        return this.time;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}