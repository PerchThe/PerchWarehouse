package dev.ev1dent.perchWarehouse.data;

import dev.ev1dent.perchWarehouse.utilities.Utils;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A crate rarity, loaded from loot/<rarity>.yml.
 *
 * Opening a crate gives every "always" entry, plus "picks" entries drawn by weight from the rest.
 * Each entry can only be drawn once per crate, unless allow-duplicates is on.
 */
public class Rarity {

    /**
     * One thing that can be inside the crate. item can be null for command-only entries.
     *
     * @param weight how likely this is to be drawn compared to the other entries
     * @param always in every crate, doesn't use up a pick
     */
    public record Entry(String key, ItemStack item, int[] amount, int weight, boolean always, List<String> commands, String message) {

        /** Builds the stacks to give, split into max-stack-size pieces. */
        public List<ItemStack> createStacks() {
            List<ItemStack> stacks = new ArrayList<>();
            if (item == null) return stacks;
            int total = Utils.rollRange(amount);
            int max = Math.max(1, item.getMaxStackSize());
            while (total > 0) {
                ItemStack stack = item.clone();
                int size = Math.min(total, max);
                stack.setAmount(size);
                stacks.add(stack);
                total -= size;
            }
            return stacks;
        }
    }

    private final String name;
    private final String displayName;
    private final int[] picks;
    private final boolean allowDuplicates;
    private final FireworkEffect firework;
    private final int fireworkPower;
    private final List<Entry> entries;

    public Rarity(String name, String displayName, int[] picks, boolean allowDuplicates,
                  FireworkEffect firework, int fireworkPower, List<Entry> entries) {
        this.name = name;
        this.displayName = displayName;
        this.picks = picks;
        this.allowDuplicates = allowDuplicates;
        this.firework = firework;
        this.fireworkPower = fireworkPower;
        this.entries = entries;
    }

    public String getName() { return name; }
    /** MiniMessage name shown in chat, e.g. "<red>Rare" */
    public String getDisplayName() { return displayName; }
    public int[] getPicks() { return picks; }
    public boolean isAllowDuplicates() { return allowDuplicates; }
    public FireworkEffect getFirework() { return firework; }
    public int getFireworkPower() { return fireworkPower; }
    public List<Entry> getEntries() { return entries; }

    /** What this crate holds this time. */
    public List<Entry> rollContents() {
        List<Entry> contents = new ArrayList<>();
        List<Entry> pool = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.always()) contents.add(entry);
            else if (entry.weight() > 0) pool.add(entry);
        }

        int count = Utils.rollRange(picks);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            int total = 0;
            for (Entry entry : pool) total += entry.weight();
            int roll = ThreadLocalRandom.current().nextInt(total);
            for (int j = 0; j < pool.size(); j++) {
                Entry entry = pool.get(j);
                roll -= entry.weight();
                if (roll < 0) {
                    contents.add(entry);
                    if (!allowDuplicates) pool.remove(j);
                    break;
                }
            }
        }
        return contents;
    }
}
