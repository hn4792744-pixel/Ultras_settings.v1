package me.uc_hussein.ultrassettings.commands;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SettingAdminTabCompleter implements TabCompleter {

    private final UltrasSettingsPlugin plugin;

    public SettingAdminTabCompleter(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ultras.settings.admin")) return List.of();

        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("reload", "open", "rest"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                options.add(p.getName());
            }
            return filter(options, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("open")) {
                return filter(plugin.getGuiRegistry().allIds(), args[1]);
            }
            if (sub.equals("rest")) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
            }
        }

        return List.of();
    }

    private List<String> filter(List<String> options, String partial) {
        String lower = partial.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
