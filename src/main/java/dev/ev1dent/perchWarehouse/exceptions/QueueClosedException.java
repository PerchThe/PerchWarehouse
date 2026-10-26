package dev.ev1dent.perchWarehouse.exceptions;

/**
 * Thrown when someone tries to join or leave while the queue isn't open.
 * The caller decides who to message (the player themself, or the staff member running /warehouse add).
 */
public class QueueClosedException extends Exception {

    public QueueClosedException(){
        super("The warehouse queue is closed");
    }

}
