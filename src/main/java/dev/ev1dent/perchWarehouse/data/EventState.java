package dev.ev1dent.perchWarehouse.data;

/**
 * The event is always in exactly one of these. Only WarehouseManager changes it.
 */
public enum EventState {
    /** waiting for the next interval */
    IDLE,
    /** players can join, queue saved to disk */
    QUEUE_OPEN,
    /** teleporting everyone and waiting for chunks, players frozen */
    LOCKING,
    /** players are in place and frozen for the countdown */
    COUNTDOWN,
    /** boxes are placed and lootable */
    RUNNING,
    /** putting players and blocks back */
    CLEANUP;

    /** true while players are inside the warehouse */
    public boolean isInProgress() {
        return this == LOCKING || this == COUNTDOWN || this == RUNNING;
    }

    /** true while players are not allowed to move */
    public boolean isFrozen() {
        return this == LOCKING || this == COUNTDOWN;
    }
}
