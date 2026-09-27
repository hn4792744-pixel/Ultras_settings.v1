package me.uc_hussein.ultrassettings.services;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import me.uc_hussein.ultrassettings.gui.BlocklistGui;
import me.uc_hussein.ultrassettings.gui.SettingsGui;
import me.uc_hussein.ultrassettings.models.GuiDefinition;
import me.uc_hussein.ultrassettings.utils.PlaceholderUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Resolves a GUI id (a file name without ".yml", e.g. "setting.gui", "blocklist.gui")
 * to the right screen. "block.gui" is intentionally not resolvable through here —
 * it always needs a specific target player, so it's only ever opened directly by
 * BlocklistGui when a head is clicked.
 */
public class GuiOpener {

    private final UltrasSettingsPlugin plugin;

    public GuiOpener(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, OfflinePlayer target, String guiId) {
        if (guiId == null || guiId.isBlank()) return;

        if (guiId.equalsIgnoreCase("blocklist.gui")) {
            new BlocklistGui(plugin, viewer, target, 0).open();
            return;
        }
        if (guiId.equalsIgnoreCase("block.gui")) {
            // Can't be opened generically — needs a specific head player. Redirect to the list instead.
            new BlocklistGui(plugin, viewer, target, 0).open();
            return;
        }

        GuiDefinition def = plugin.getGuiRegistry().get(guiId);
        if (def == null) {
            plugin.getLanguageManager().send(viewer, "general.gui-not-found", PlaceholderUtil.create().put("gui", guiId));
            return;
        }
        new SettingsGui(plugin, viewer, target, def).open();
    }
}
