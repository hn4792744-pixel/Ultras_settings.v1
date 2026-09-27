package me.uc_hussein.ultrassettings.listeners;

import me.uc_hussein.ultrassettings.gui.ClickableGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Every Ultras_settings GUI is strictly read-only (spec section 31): every click
 * and every drag touching one of our inventories is cancelled unconditionally,
 * regardless of click type (shift-click, hotbar swap, double-click, number key,
 * drop, collect-to-cursor all move zero items because the event never proceeds).
 */
public class InventoryListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        boolean ours = holder instanceof ClickableGui;

        // Also cancel clicks into the player's OWN inventory while one of our GUIs is open on top,
        // so a shift-click from the hotbar can't sneak an item into our (item-only) GUI either.
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (!ours && !(topHolder instanceof ClickableGui)) return;

        event.setCancelled(true);
        if (ours && event.getWhoClicked() instanceof Player player) {
            ((ClickableGui) holder).handleClick(event, player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (!(topHolder instanceof ClickableGui gui)) return;
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            gui.handleDrag(event, player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ClickableGui gui)) return;
        if (event.getPlayer() instanceof Player player) {
            gui.handleClose(player);
        }
    }
}
