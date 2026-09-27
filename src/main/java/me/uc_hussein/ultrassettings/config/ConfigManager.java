package me.uc_hussein.ultrassettings.config;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/** Defensive typed accessors over config.yml — every getter has a safe default. */
public class ConfigManager {

    private final UltrasSettingsPlugin plugin;

    public ConfigManager(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public int getInt(String path, int def) { return cfg().getInt(path, def); }
    public boolean getBoolean(String path, boolean def) { return cfg().getBoolean(path, def); }
    public String getString(String path, String def) { return cfg().getString(path, def); }
    public double getDouble(String path, double def) { return cfg().getDouble(path, def); }
    public java.util.List<Integer> getIntList(String path) { return cfg().getIntegerList(path); }
    public java.util.List<String> getStringList(String path) { return cfg().getStringList(path); }

    public String getLanguage() { return cfg().getString("language", "en"); }
    public boolean isDebug() { return cfg().getBoolean("debug", false); }

    public boolean isIntegrationEnabled() { return cfg().getBoolean("integration.ultras-tpa.enabled", true); }
    public String getIntegrationPluginName() { return cfg().getString("integration.ultras-tpa.plugin-name", "ULTRAS_TPA"); }
    public String getIntegrationDataFile() { return cfg().getString("integration.ultras-tpa.data-file", "player-data.yml"); }
    public String getIntegrationCommand(String key) { return cfg().getString("integration.ultras-tpa.commands." + key, null); }
    public String getIntegrationDataKey(String key) { return cfg().getString("integration.ultras-tpa.data-keys." + key, null); }

    public boolean getDefault(String key) { return cfg().getBoolean("defaults." + key, false); }
}
