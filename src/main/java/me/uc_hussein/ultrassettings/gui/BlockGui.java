package me.uc_hussein.ultrassettings.gui;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import me.uc_hussein.ultrassettings.models.ButtonDefinition;
import me.uc_hussein.ultrassettings.models.GuiDefinition;
import me.uc_hussein.ultrassettings.models.VisualState;
import me.uc_hussein.ultrassettings.utils.ColorUtil;
import me.uc_hussein.ultrassettings.utils.ItemBuilder;
import me.uc_hussein.ultrassettings.utils.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Block/Unblock a specific player, on behalf of {@code settingsOwner}. Both
 * buttons are always present but only the currently-valid one is clickable —
 * the other renders its "inactive" state and does nothing on click, so a
 * double-click or a stale-state click can never send Block-on-an-already-
 * blocked-player (or the equivalent for Unblock) — see spec section 15/79.
 */
public class BlockGui implements ClickableGui {

    private static final int SLOT_BLOCK_BUTTON_ID = 1;
    private static final int SLOT_UNBLOCK_BUTTON_ID = 2;

    private final UltrasSettingsPlugin plugin;
    private final Player viewer;
    private final OfflinePlayer settingsOwner;
    private final OfflinePlayer headPlayer;
    private final BlocklistGui previousGui; // may be null
    private final GuiDefinition def;
    private final Inventory inventory;
    private final Map<Integer, Integer> slotRoles = new HashMap<>(); // slot -> SLOT_BLOCK_BUTTON_ID / SLOT_UNBLOCK_BUTTON_ID / -1=back
    private final Map<Integer, ButtonDefinition> rawButtons = new HashMap<>();

    public BlockGui(UltrasSettingsPlugin plugin, Player viewer, OfflinePlayer settingsOwner,
                     OfflinePlayer headPlayer, BlocklistGui previousGui) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.settingsOwner = settingsOwner;
        this.headPlayer = headPlayer;
        this.previousGui = previousGui;
        this.def = plugin.getGuiRegistry().get("block.gui");
        this.inventory = Bukkit.createInventory(this, def != null ? def.size : 27,
                ColorUtil.render(def != null ? def.title : "Manage Player"));
        render();
    }

    public void open() {
        viewer.openInventory(inventory);
        if (def != null && def.soundOpen != null) {
            plugin.getSoundService().play(viewer, def.soundOpen);
        }
    }

    private boolean isBlocked() {
        return plugin.getTpaIntegration().isBlocked(settingsOwner.getUniqueId(), headPlayer.getUniqueId());
    }

    private void render() {
        if (def == null) return;
        inventory.clear();
        slotRoles.clear();
        rawButtons.clear();

        if (def.decoration != null) {
            ItemStack deco = ItemBuilder.decoration(def.decoration.material, def.decoration.name);
            for (int slot : def.decoration.slots) {
                if (slot >= 0 && slot < def.size) inventory.setItem(slot, deco);
            }
        }

        boolean blocked = isBlocked();
        PlaceholderUtil placeholders = PlaceholderUtil.create()
                .put("player", headPlayer.getName() != null ? headPlayer.getName() : "?")
                .put("uuid", headPlayer.getUniqueId())
                .put("online", headPlayer.isOnline() ? "yes" : "no")
                .put("status", plugin.getLanguageManager().raw(blocked ? "block.status-blocked" : "block.status-not-blocked"));

        if (def.playerHeadTemplate != null && def.playerHeadSlot >= 0) {
            ItemStack head = ItemBuilder.buildSkull(headPlayer, def.playerHeadTemplate, placeholders);
            inventory.setItem(def.playerHeadSlot, head);
        }

        for (ButtonDefinition button : def.buttons.values()) {
            if (button.slot < 0 || button.slot >= def.size) continue;
            switch (button.type) {
                case BLOCK_STATUS -> {
                    VisualState state = !blocked ? button.active : button.inactive; // clickable ("active") only if NOT already blocked
                    inventory.setItem(button.slot, ItemBuilder.build(state, placeholders));
                    slotRoles.put(button.slot, SLOT_BLOCK_BUTTON_ID);
                    rawButtons.put(button.slot, button);
                }
                case UNBLOCK_STATUS -> {
                    VisualState state = blocked ? button.active : button.inactive; // clickable only if currently blocked
                    inventory.setItem(button.slot, ItemBuilder.build(state, placeholders));
                    slotRoles.put(button.slot, SLOT_UNBLOCK_BUTTON_ID);
                    rawButtons.put(button.slot, button);
                }
                case ACTION -> {
                    inventory.setItem(button.slot, ItemBuilder.build(button.single, placeholders));
                    slotRoles.put(button.slot, -1);
                    rawButtons.put(button.slot, button);
                }
                default -> {}
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event, Player clicker) {
        if (def == null) return;
        int slot = event.getRawSlot();
        Integer role = slotRoles.get(slot);
        if (role == null) return;
        ButtonDefinition button = rawButtons.get(slot);

        if (role == -1) { // back
            if (button.action == me.uc_hussein.ultrassettings.models.ActionType.BACK
                    || button.action == me.uc_hussein.ultrassettings.models.ActionType.OPEN_GUI) {
                VisualState state = button.single;
                if (state.sound != null) plugin.getSoundService().play(clicker, state.sound, state.volume, state.pitch);
                if (previousGui != null) {
                    previousGui.open(); // preserves the page the admin/player was on
                } else {
                    plugin.getGuiOpener().open(viewer, settingsOwner, "blocklist.gui");
                }
            }
            return;
        }

        boolean blocked = isBlocked();
        if (role == SLOT_BLOCK_BUTTON_ID) {
            if (blocked) {
                plugin.getLanguageManager().send(viewer, "block.already-blocked",
                        PlaceholderUtil.create().put("target", headPlayer.getName()));
                plugin.getSoundService().play(viewer, "error");
                return;
            }
            executeBlockAction(button.active, "block");
        } else if (role == SLOT_UNBLOCK_BUTTON_ID) {
            if (!blocked) {
                plugin.getLanguageManager().send(viewer, "block.not-blocked",
                        PlaceholderUtil.create().put("target", headPlayer.getName()));
                plugin.getSoundService().play(viewer, "error");
                return;
            }
            executeBlockAction(button.active, "unblock");
        }
        render();
    }

    private void executeBlockAction(VisualState state, String commandKey) {
        if (!plugin.getTpaIntegration().isAvailable()) {
            plugin.getLanguageManager().send(viewer, "integration.not-installed");
            plugin.getSoundService().play(viewer, "error");
            return;
        }
        Player actor = settingsOwner.getUniqueId().equals(viewer.getUniqueId()) ? viewer : Bukkit.getPlayer(settingsOwner.getUniqueId());
        if (actor == null) {
            plugin.getLanguageManager().send(viewer, "admin.target-offline",
                    PlaceholderUtil.create().put("target", settingsOwner.getName()));
            plugin.getSoundService().play(viewer, "error");
            return;
        }
        String command = plugin.getTpaIntegration().getCommand(commandKey);
        PlaceholderUtil placeholders = PlaceholderUtil.create().put("target", headPlayer.getName());
        plugin.getTpaIntegration().dispatchAsPlayer(actor, command, placeholders);
        if (state != null && state.sound != null) plugin.getSoundService().play(viewer, state.sound, state.volume, state.pitch);
    }
}
