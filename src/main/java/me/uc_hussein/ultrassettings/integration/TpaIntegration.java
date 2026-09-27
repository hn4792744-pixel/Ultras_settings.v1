package me.uc_hussein.ultrassettings.integration;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import me.uc_hussein.ultrassettings.utils.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * The ONLY place Ultras_settings knows anything about Ultras_TPA's internals.
 * Never writes to Ultras_TPA's files (that plugin owns its own data — this
 * class only reads it, to display accurate ON/OFF state), and never touches
 * Ultras_TPA's commands/classes/NMS — every write goes through Bukkit's own
 * Server#dispatchCommand(CommandSender, String) with a real Player as the
 * sender, exactly like a player typing the command themselves. If Ultras_TPA
 * ever ships a proper API, only this class needs to change.
 */
public class TpaIntegration {

    private final UltrasSettingsPlugin plugin;

    public TpaIntegration(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        if (!plugin.getConfigManager().isIntegrationEnabled()) return false;
        Plugin tpa = Bukkit.getPluginManager().getPlugin(plugin.getConfigManager().getIntegrationPluginName());
        return tpa != null && tpa.isEnabled();
    }

    private File dataFile() {
        Plugin tpa = Bukkit.getPluginManager().getPlugin(plugin.getConfigManager().getIntegrationPluginName());
        if (tpa == null) return null;
        return new File(tpa.getDataFolder(), plugin.getConfigManager().getIntegrationDataFile());
    }

    /**
     * Fresh read every call — this file is small and only read on GUI open/refresh
     * (never per-tick), so staleness is a bigger risk here than the I/O cost.
     */
    private YamlConfiguration readDoc() {
        File file = dataFile();
        if (file == null || !file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    /** True/false state for a toggle-source ("tpa", "tpahere", "tpaall", "auto"), falling back to our own configured default. */
    public boolean readToggle(UUID uuid, String toggleSource) {
        String dataKey = plugin.getConfigManager().getIntegrationDataKey(toggleSource);
        boolean fallback = plugin.getConfigManager().getDefault(toggleSource);
        if (dataKey == null) return fallback;
        YamlConfiguration doc = readDoc();
        if (doc == null) return fallback;
        return doc.getBoolean("players." + uuid + "." + dataKey, fallback);
    }

    public boolean isBlocked(UUID owner, UUID target) {
        String dataKey = plugin.getConfigManager().getIntegrationDataKey("blocked-list");
        if (dataKey == null) return false;
        YamlConfiguration doc = readDoc();
        if (doc == null) return false;
        List<String> blocked = doc.getStringList("players." + owner + "." + dataKey);
        return blocked.contains(target.toString());
    }

    /**
     * Executes {@code commandTemplate} (e.g. "ultras_tpa:tpa toggle" or "ultras_tpa:tpa block %target%")
     * with {@code actor} as the command sender — the standard, safe way to run a command "as" a
     * specific online player via public Bukkit API, no reflection or NMS involved.
     */
    public void dispatchAsPlayer(Player actor, String commandTemplate, PlaceholderUtil placeholders) {
        if (commandTemplate == null || commandTemplate.isBlank()) return;
        String resolved = placeholders != null ? placeholders.apply(commandTemplate) : commandTemplate;
        if (resolved.startsWith("/")) resolved = resolved.substring(1);
        Bukkit.dispatchCommand(actor, resolved);
    }

    public void dispatchAsConsole(String commandTemplate, PlaceholderUtil placeholders) {
        if (commandTemplate == null || commandTemplate.isBlank()) return;
        String resolved = placeholders != null ? placeholders.apply(commandTemplate) : commandTemplate;
        if (resolved.startsWith("/")) resolved = resolved.substring(1);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
    }

    public String getCommand(String key) {
        return plugin.getConfigManager().getIntegrationCommand(key);
    }
}
