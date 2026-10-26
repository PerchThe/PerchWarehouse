package dev.ev1dent.perchWarehouse.listeners;

import dev.ev1dent.perchWarehouse.WarehousePlugin;
import dev.ev1dent.perchWarehouse.data.MarkerType;
import dev.ev1dent.perchWarehouse.managers.EditorManager;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EditorListener implements Listener {
    private WarehousePlugin warehousePlugin(){
        return WarehousePlugin.getPlugin(WarehousePlugin.class);
    }
    EditorManager editorManager = warehousePlugin().getEditorManager();

    /** Placing a marker block adds the marker; the real block is never placed. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!editorManager.isEditorItem(item)) return;
        event.setCancelled(true);
        if (!editorManager.isEditing(event.getPlayer())) return;
        MarkerType type = editorManager.getMarkerType(item);
        if (type != null) editorManager.placeMarker(event.getPlayer(), event.getBlockPlaced(), type);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!editorManager.isExitItem(event.getItem())) return;
        event.setCancelled(true);
        editorManager.exit(event.getPlayer());
    }

    /** Punch a marker to remove it. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttackMarker(PrePlayerAttackEntityEvent event) {
        if (!editorManager.isMarkerEntity(event.getAttacked())) return;
        event.setCancelled(true);
        editorManager.removeMarker(event.getPlayer(), event.getAttacked());
    }

    /** Right-clicking a marker would otherwise eat the click, so tell them how to remove it. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractMarker(PlayerInteractEntityEvent event) {
        if (!editorManager.isMarkerEntity(event.getRightClicked())) return;
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND && event.getPlayer().isSneaking())
            editorManager.removeMarker(event.getPlayer(), event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (editorManager.isEditorItem(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    /** Keep marker items out of chests etc. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !editorManager.isEditing(player)) return;
        InventoryType top = event.getView().getTopInventory().getType();
        if (top == InventoryType.CRAFTING || top == InventoryType.CREATIVE) return;
        if (editorManager.isEditorItem(event.getCurrentItem()) || editorManager.isEditorItem(event.getCursor()))
            event.setCancelled(true);
    }

    /** Don't drop marker items (or the backed-up inventory's replacement) if an editor dies. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (!editorManager.isEditing(event.getEntity())) return;
        event.getDrops().removeIf(editorManager::isEditorItem);
    }
}
