package me.uc_hussein.ultrassettings.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ColorUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ColorUtil() {}

    public static Component render(String raw) {
        if (raw == null) return Component.empty();
        return MM.deserialize(translateLegacy(raw));
    }

    private static String translateLegacy(String input) {
        if (input.indexOf('&') < 0) return input;
        return input
                .replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&l", "<bold>").replace("&n", "<underlined>")
                .replace("&o", "<italic>").replace("&m", "<strikethrough>").replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
    }
}
