package me.uc_hussein.ultrassettings.language;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import me.uc_hussein.ultrassettings.utils.ColorUtil;
import me.uc_hussein.ultrassettings.utils.PlaceholderUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Loads lang/en.yml and lang/ar.yml. All player-facing feedback text lives here — see README. */
public class LanguageManager {

    private final UltrasSettingsPlugin plugin;
    private YamlConfiguration active;
    private YamlConfiguration fallback;

    public LanguageManager(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        load();
    }

    private void load() {
        this.fallback = loadBundled("lang/en.yml");
        String lang = plugin.getConfigManager().getLanguage();
        File external = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (external.exists()) {
            this.active = YamlConfiguration.loadConfiguration(external);
        } else {
            YamlConfiguration bundled = loadBundled("lang/" + lang + ".yml");
            this.active = bundled != null ? bundled : fallback;
        }
        if (this.active == null) this.active = fallback;
    }

    private YamlConfiguration loadBundled(String path) {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load " + path + ": " + e.getMessage());
            return null;
        }
    }

    public String raw(String key) {
        String value = active.getString(key);
        if (value == null) value = fallback.getString(key);
        return value != null ? value : "Missing message: " + key;
    }

    private String prefixRaw() {
        String value = active.getString("prefix");
        if (value == null) value = fallback.getString("prefix");
        return value != null ? value : "";
    }

    public Component get(String key, PlaceholderUtil placeholders) {
        String body = placeholders != null ? placeholders.apply(raw(key)) : raw(key);
        return ColorUtil.render(body);
    }

    public Component getPrefixed(String key, PlaceholderUtil placeholders) {
        String prefix = placeholders != null ? placeholders.apply(prefixRaw()) : prefixRaw();
        String body = placeholders != null ? placeholders.apply(raw(key)) : raw(key);
        return ColorUtil.render(prefix + body);
    }

    public void send(CommandSender target, String key) {
        target.sendMessage(getPrefixed(key, null));
    }

    public void send(CommandSender target, String key, PlaceholderUtil placeholders) {
        target.sendMessage(getPrefixed(key, placeholders));
    }
}
