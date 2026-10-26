package dev.ev1dent.perchWarehouse.configuration;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.data.Rarity;
import dev.ev1dent.perchWarehouse.utilities.Utils;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads every rarity from plugins/PerchWarehouse/loot/<rarity>.yml. The file name is the rarity name.
 * Every bad entry is logged with its file and path and skipped instead of breaking the plugin.
 */
public class LootManager {

    private static final String[] DEFAULT_FILES = {"common.yml", "uncommon.yml", "rare.yml"};

    private final WarehousePlugin plugin;
    private final File folder;
    private final Map<String, Rarity> rarities = new LinkedHashMap<>();

    public LootManager(WarehousePlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "loot");
        // only on first install, so deleting a default rarity doesn't bring it back
        if (!folder.exists()) {
            folder.mkdirs();
            for (String name : DEFAULT_FILES) plugin.saveResource("loot/" + name, false);
        }
        reload();
    }

    public void reload() {
        rarities.clear();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("loot/ has no rarity files, crates will be empty");
            return;
        }
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            rarities.put(name, parseRarity(file.getName(), name, YamlConfiguration.loadConfiguration(file)));
        }
        Utils.getDebugLogger("Loaded " + rarities.size() + " rarities: " + String.join(", ", rarities.keySet()));
    }

    /** case-insensitive, null if it doesn't exist */
    public Rarity getRarity(String name) {
        return name == null ? null : rarities.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Rarity> getRarities() {
        return rarities.values();
    }

    private Rarity parseRarity(String file, String name, ConfigurationSection config) {
        String displayName = config.getString("display-name", name);

        int[] picks;
        try {
            picks = Utils.parseRange(config.getString("picks", "1"));
        } catch (IllegalArgumentException e) {
            warn(file, "picks", e.getMessage() + ", using 1");
            picks = new int[]{1, 1};
        }
        boolean allowDuplicates = config.getBoolean("allow-duplicates", false);

        FireworkEffect firework = null;
        int power = 0;
        ConfigurationSection fw = config.getConfigurationSection("firework");
        if (fw != null && fw.getBoolean("enabled", true)) {
            firework = parseFirework(file, "firework", fw);
            power = Math.max(0, Math.min(3, fw.getInt("power", 0)));
        }

        List<Rarity.Entry> entries = new ArrayList<>();
        ConfigurationSection contents = config.getConfigurationSection("contents");
        if (contents == null) {
            warn(file, "contents", "is missing, this crate will be empty");
        } else {
            for (String key : contents.getKeys(false)) {
                ConfigurationSection section = contents.getConfigurationSection(key);
                if (section == null) {
                    warn(file, "contents." + key, "is not a section, skipping");
                    continue;
                }
                Rarity.Entry entry = parseEntry(file, "contents." + key, key, section);
                if (entry != null) entries.add(entry);
            }
        }
        long drawable = entries.stream().filter(e -> !e.always() && e.weight() > 0).count();
        if (!allowDuplicates && picks[1] > drawable && drawable > 0)
            warn(file, "picks", "can be up to " + picks[1] + " but only " + drawable
                + " entries can be drawn, so crates will just give all of them");
        return new Rarity(name, displayName, picks, allowDuplicates, firework, power, entries);
    }

    private Rarity.Entry parseEntry(String file, String path, String key, ConfigurationSection section) {
        boolean always = section.getBoolean("always", false);
        int weight = section.getInt("weight", 1);
        if (weight < 0) {
            warn(file, path + ".weight", "can't be negative, using 1");
            weight = 1;
        }
        if (section.contains("chance"))
            warn(file, path + ".chance", "chance was replaced by weight (and always: true), ignoring it");
        List<String> commands = section.getStringList("commands");
        String message = section.getString("message");

        ItemStack item = null;
        String raw = section.getString("item");
        String materialName = section.getString("material");
        if (raw != null) {
            try {
                item = Bukkit.getItemFactory().createItemStack(raw);
            } catch (IllegalArgumentException e) {
                warn(file, path + ".item", "'" + raw + "' could not be parsed (" + e.getMessage() + "), skipping this item");
                return null;
            }
        } else if (materialName != null) {
            Material material = Material.matchMaterial(materialName);
            if (material == null || !material.isItem() || material.isAir()) {
                warn(file, path + ".material", "'" + materialName + "' is not an item, skipping this item");
                return null;
            }
            item = new ItemStack(material);
        } else if (commands.isEmpty()) {
            warn(file, path, "needs a material, an item, or commands, skipping this item");
            return null;
        }

        int[] amount = {1, 1};
        if (section.contains("amount")) {
            try {
                amount = Utils.parseRange(section.getString("amount"));
            } catch (IllegalArgumentException e) {
                warn(file, path + ".amount", e.getMessage() + ", using 1");
            }
        }

        if (item != null) applyMeta(file, path, item, section);
        return new Rarity.Entry(key, item, amount, weight, always, commands, message);
    }

    private void applyMeta(String file, String path, ItemStack item, ConfigurationSection section) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (section.contains("name")) meta.displayName(Utils.itemText(section.getString("name")));
        if (section.contains("lore")) {
            List<Component> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) lore.add(Utils.itemText(line));
            meta.lore(lore);
        }

        ConfigurationSection enchants = section.getConfigurationSection("enchantments");
        if (enchants != null) {
            Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
            for (String name : enchants.getKeys(false)) {
                NamespacedKey enchantKey = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
                Enchantment enchantment = enchantKey == null ? null : registry.get(enchantKey);
                if (enchantment == null) {
                    warn(file, path + ".enchantments." + name, "is not an enchantment, skipping it");
                    continue;
                }
                meta.addEnchant(enchantment, Math.max(1, enchants.getInt(name, 1)), true);
            }
        }

        if (section.contains("glow")) meta.setEnchantmentGlintOverride(section.getBoolean("glow"));
        if (section.contains("unbreakable")) meta.setUnbreakable(section.getBoolean("unbreakable"));
        if (section.contains("custom-model-data")) setCustomModelData(meta, section.getInt("custom-model-data"));
        item.setItemMeta(meta);
    }

    @SuppressWarnings("deprecation")
    private void setCustomModelData(ItemMeta meta, int data) {
        meta.setCustomModelData(data);
    }

    private FireworkEffect parseFirework(String file, String path, ConfigurationSection section) {
        FireworkEffect.Type type = ConfigManager.parseEnum(FireworkEffect.Type.class, section.getString("type", "BALL"));
        if (type == null) {
            warn(file, path + ".type", "'" + section.getString("type") + "' is not a firework type, using BALL");
            type = FireworkEffect.Type.BALL;
        }
        List<Color> colors = parseColors(file, path + ".colors", section.getStringList("colors"));
        if (colors.isEmpty()) {
            warn(file, path + ".colors", "needs at least one color, using WHITE");
            colors.add(Color.WHITE);
        }
        List<Color> fade = parseColors(file, path + ".fade", section.getStringList("fade"));
        return FireworkEffect.builder()
            .with(type)
            .withColor(colors)
            .withFade(fade)
            .flicker(section.getBoolean("flicker", false))
            .trail(section.getBoolean("trail", false))
            .build();
    }

    private List<Color> parseColors(String file, String path, List<String> raw) {
        List<Color> colors = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            try {
                colors.add(Utils.parseColor(raw.get(i)));
            } catch (IllegalArgumentException e) {
                warn(file, path + "[" + i + "]", e.getMessage() + ", skipping it");
            }
        }
        return colors;
    }

    private void warn(String file, String path, String problem) {
        plugin.getLogger().warning("loot/" + file + " -> " + path + ": " + problem);
    }
}
