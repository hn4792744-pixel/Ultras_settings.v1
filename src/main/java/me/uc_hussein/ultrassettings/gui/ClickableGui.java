package me.uc_hussein.ultrassettings.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/** Every Ultras_settings GUI is its own read-only InventoryHolder. */
public interface ClickableGui extends InventoryHolder {

    /** InventoryListener has already cancelled the event before calling this. */
    void handleClick(InventoryClickEvent event, Player viewer);

    /** All drag events into one of our GUIs are cancelled unconditionally — this is just a notification hook. */
    default void handleDrag(InventoryDragEvent event, Player viewer) {}

    default void handleClose(Player viewer) {}
}
