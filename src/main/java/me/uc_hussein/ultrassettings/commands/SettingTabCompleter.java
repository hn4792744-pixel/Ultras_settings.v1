package me.uc_hussein.ultrassettings.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.stream.Collectors;

public class SettingTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String partial = args[0].toLowerCase();
        return List.of("rest").stream().filter(o -> o.startsWith(partial)).collect(Collectors.toList());
    }
}
