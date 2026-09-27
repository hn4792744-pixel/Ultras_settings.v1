package me.uc_hussein.ultrassettings.commands;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import me.uc_hussein.ultrassettings.utils.PlaceholderUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SettingCommand implements CommandExecutor {

    private final UltrasSettingsPlugin plugin;

    public SettingCommand(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLanguageManager().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("ultras.settings.use")) {
            plugin.getLanguageManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("rest")) {
            int changed = plugin.getSettingsService().resetToDefaults(player);
            plugin.getLanguageManager().send(player, "settings.reset-success");
            plugin.getSoundService().play(player, "reset");
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("[debug] " + player.getName() + " reset their own settings (" + changed + " changed).");
            }
            return true;
        }

        if (args.length >= 1) {
            plugin.getLanguageManager().send(player, "general.invalid-usage",
                    PlaceholderUtil.create().put("usage", "/setting [rest]"));
            return true;
        }

        plugin.getGuiOpener().open(player, player, "setting.gui");
        return true;
    }
}
