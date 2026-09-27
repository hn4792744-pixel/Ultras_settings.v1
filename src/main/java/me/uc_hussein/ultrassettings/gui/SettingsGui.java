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
 * Renders any GUI file that only uses TOGGLE / ACTION / COMING_SOON buttons —
 * every setting.gui*.yml file, and any custom one added later purely via YAML.
 * "viewer" is who's holding the inventory; "target" is whose settings are being
 * shown/changed (equal to viewer for a normal /setting, different for
 * /setting_admin <player>).
 */
public class SettingsGui implements ClickableGui {

    private final UltrasSettingsPlugin plugin;
    private final Player viewer;
    private final OfflinePlayer target;
    private final GuiDefinition def;
    private final Inventory inventory;
    private final Map<Integer, ButtonDefinition> slotMap = new HashMap<>();

    public SettingsGui(UltrasSettingsPlugin plugin, Player viewer, OfflinePlayer target, GuiDefinition def) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.target = target;
        this.def = def;
        this.inventory = Bukkit.createInventory(this, def.size, ColorUtil.render(resolvedTitle()));
        render();
    }

    private String resolvedTitle() {
        boolean adminView = !target.getUniqueId().equals(viewer.getUniqueId());
        String title = def.title;
        if (adminView) {
            title = title + " <dark_gray>—</dark_gray> <white>" + target.getName();
        }
        return title;
    }

    public void open() {
        viewer.openInventory(inventory);
        if (def.soundOpen != null) {
            plugin.getSoundService().play(viewer, def.soundOpen);
        }
    }

    private void render() {
        inventory.clear();
        slotMap.clear();

        if (def.decoration != null) {
            ItemStack deco = ItemBuilder.decoration(def.decoration.material, def.decoration.name);
            for (int slot : def.decoration.slots) {
                if (slot >= 0 && slot < def.size) inventory.setItem(slot, deco);
            }
        }

        PlaceholderUtil placeholders = PlaceholderUtil.create()
                .put("player", viewer.getName())
                .put("target", target.getName() != null ? target.getName() : "?");

        for (ButtonDefinition button : def.buttons.values()) {
            if (button.permission != null && !viewer.hasPermission(button.permission)) continue;
            if (button.slot < 0 || button.slot >= def.size) continue;

            VisualState state = resolveVisual(button);
            ItemStack item = ItemBuilder.build(state, placeholders);
            inventory.setItem(button.slot, item);
            slotMap.put(button.slot, button);
        }
    }

    private VisualState resolveVisual(ButtonDefinition button) {
        return switch (button.type) {
            case TOGGLE -> currentToggleState(button) ? button.on : button.off;
            case ACTION, COMING_SOON -> button.single;
            default -> button.single != null ? button.single : new VisualState();
        };
    }

    private boolean currentToggleState(ButtonDefinition button) {
        if (!plugin.getTpaIntegration().isAvailable()) return false;
        return plugin.getTpaIntegration().readToggle(target.getUniqueId(), button.toggleSource);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event, Player clicker) {
        ButtonDefinition button = slotMap.get(event.getRawSlot());
        if (button == null) return;
        if (button.permission != null && !clicker.hasPermission(button.permission)) return;

        switch (button.type) {
            case TOGGLE -> handleToggleClick(button);
            case COMING_SOON -> handleComingSoon(button);
            case ACTION -> handleAction(button);
            default -> {}
        }
    }

    private void handleToggleClick(ButtonDefinition button) {
        if (!plugin.getTpaIntegration().isAvailable()) {
            plugin.getLanguageManager().send(viewer, "integration.not-installed");
            plugin.getSoundService().play(viewer, "error");
            return;
        }
        Player actor = resolveActor();
        if (actor == null) {
            plugin.getLanguageManager().send(viewer, "admin.target-offline",
                    PlaceholderUtil.create().put("target", target.getName()));
            plugin.getSoundService().play(viewer, "error");
            return;
        }

        boolean before = currentToggleState(button);
        VisualState state = before ? button.on : button.off;
        plugin.getTpaIntegration().dispatchAsPlayer(actor, state.command, null);
        if (state.sound != null) plugin.getSoundService().play(viewer, state.sound, state.volume, state.pitch);
        if (state.message != null && !state.message.isBlank()) {
            viewer.sendMessage(ColorUtil.render(state.message));
        }
        render(); // reflect the new real state read fresh from Ultras_TPA's data
    }

    private void handleComingSoon(ButtonDefinition button) {
        VisualState state = button.single;
        String message = state.message != null && !state.message.isBlank()
                ? state.message : plugin.getLanguageManager().raw("buttons.coming-soon");
        viewer.sendMessage(ColorUtil.render(message));
        if (state.sound != null) plugin.getSoundService().play(viewer, state.sound, state.volume, state.pitch);
    }

    private void handleAction(ButtonDefinition button) {
        VisualState state = button.single;
        if (state.sound != null) plugin.getSoundService().play(viewer, state.sound, state.volume, state.pitch);

        switch (button.action) {
            case CLOSE -> viewer.closeInventory();
            case OPEN_GUI, BACK -> {
                if (button.targetGui != null) {
                    plugin.getGuiOpener().open(viewer, target, button.targetGui);
                }
            }
            case MESSAGE -> {
                if (state.message != null) viewer.sendMessage(ColorUtil.render(state.message));
            }
            case PLAYER_COMMAND, TOGGLE -> {
                Player actor = resolveActor();
                if (actor == null) {
                    plugin.getLanguageManager().send(viewer, "admin.target-offline",
                            PlaceholderUtil.create().put("target", target.getName()));
                    return;
                }
                plugin.getTpaIntegration().dispatchAsPlayer(actor, state.command,
                        PlaceholderUtil.create().put("target", target.getName()));
                if (state.message != null && !state.message.isBlank()) {
                    viewer.sendMessage(ColorUtil.render(state.message));
                }
                render();
            }
            case CONSOLE_COMMAND -> {
                plugin.getTpaIntegration().dispatchAsConsole(state.command,
                        PlaceholderUtil.create().put("target", target.getName()));
                if (state.message != null && !state.message.isBlank()) {
                    viewer.sendMessage(ColorUtil.render(state.message));
                }
            }
            case NEXT_PAGE, PREVIOUS_PAGE, BLOCK, UNBLOCK, NONE -> {
                // Not meaningful on a generic settings page — BlocklistGui/BlockGui implement these directly.
            }
        }
    }

    /** Who a command should be dispatched as: the viewer for their own settings, or the online target for admin view. */
    private Player resolveActor() {
        if (target.getUniqueId().equals(viewer.getUniqueId())) return viewer;
        return Bukkit.getPlayer(target.getUniqueId());
    }
}
