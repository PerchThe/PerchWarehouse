package dev.ev1dent.perchWarehouse.data;

/**
 * A crate that is currently in the world during an event.
 * previousBlock is the block data string that was there before, so cleanup puts it back instead of setting air.
 */
public record PlacedBox(BlockKey key, MarkerType spot, Rarity rarity, String previousBlock) {}
