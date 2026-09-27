package me.uc_hussein.ultrassettings.commands;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import me.uc_hussein.ultrassettings.utils.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SettingAdminCommand implements CommandExecutor {

    private final UltrasSettingsPlugin plugin;

    public SettingAdminCommand(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultras.settings.admin")) {
            plugin.getLanguageManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.getLanguageManager().send(sender, "general.invalid-usage",
                    PlaceholderUtil.create().put("usage", "/setting_admin <reload|open <gui>|rest <player>|player>"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "open" -> handleOpen(sender, args.length >= 2 ? args[1] : null);
            case "rest" -> handleReset(sender, args.length >= 2 ? args[1] : null);
            default -> handleOpenPlayerSettings(sender, args[0]);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("ultras.settings.admin.reload")) {
            plugin.getLanguageManager().send(sender, "general.no-permission");
            return;
        }
        plugin.reloadEverything();
        plugin.getLanguageManager().send(sender, "general.reload-success");
    }

    private void handleOpen(CommandSender sender, String guiId) {
        if (!sender.hasPermission("ultras.settings.admin.open")) {
            plugin.getLanguageManager().send(sender, "general.no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.getLanguageManager().send(sender, "general.player-only");
            return;
        }
        if (guiId == null) {
            plugin.getLanguageManager().send(sender, "general.invalid-usage",
                    PlaceholderUtil.create().put("usage", "/setting_admin open <gui>"));
            return;
        }
        plugin.getGuiOpener().open(player, player, guiId);
    }

    private void handleReset(CommandSender sender, String targetName) {
        if (!sender.hasPermission("ultras.settings.admin.reset")) {
            plugin.getLanguageManager().send(sender, "general.no-permission");
            return;
        }
        if (targetName == null) {
            plugin.getLanguageManager().send(sender, "general.invalid-usage",
                    PlaceholderUtil.create().put("usage", "/setting_admin rest <player>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            plugin.getLanguageManager().send(sender, "admin.target-offline",
                    PlaceholderUtil.create().put("target", targetName));
            return;
        }
        plugin.getSettingsService().resetToDefaults(target);
        plugin.getLanguageManager().send(sender, "settings.reset-success-admin",
                PlaceholderUtil.create().put("target", target.getName()));
    }

    private void handleOpenPlayerSettings(CommandSender sender, String targetName) {
        if (!sender.hasPermission("ultras.settings.admin.player")) {
            plugin.getLanguageManager().send(sender, "general.no-permission");
            return;
        }
        if (!(sender instanceof Player admin)) {
            plugin.getLanguageManager().send(sender, "general.player-only");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            plugin.getLanguageManager().send(sender, "general.player-not-found");
            return;
        }
        plugin.getGuiOpener().open(admin, target, "setting.gui");
    }
}
