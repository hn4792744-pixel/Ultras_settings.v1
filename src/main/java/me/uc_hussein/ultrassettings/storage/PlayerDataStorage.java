package me.uc_hussein.ultrassettings.storage;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Genuine lazy loading (spec sections 22/23): nothing is read from disk until a
 * specific player's data is actually asked for, and it's evicted from memory
 * after being saved once that player leaves — Ultras_settings never holds every
 * player's data in RAM at once. Currently unused by any shipped button (every
 * TPA-related toggle lives in Ultras_TPA's own file, read-only, via
 * TpaIntegration) — this exists so a future Ultras_settings-owned setting
 * (replacing a "COMING SOON" button) has somewhere safe to persist without
 * redesigning the storage layer.
 */
public class PlayerDataStorage {

    private final UltrasSettingsPlugin plugin;
    private final File baseDir;
    private final Map<UUID, YamlConfiguration> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> dirty = new ConcurrentHashMap<>();

    public PlayerDataStorage(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "data/players");
        baseDir.mkdirs();
    }

    private File fileFor(UUID uuid) {
        return new File(baseDir, uuid.toString() + ".yml");
    }

    /** Loads on first access for this player, then serves from memory until unload(). */
    public YamlConfiguration get(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            File f = fileFor(id);
            if (f.exists()) {
                try {
                    return YamlConfiguration.loadConfiguration(f);
                } catch (Exception e) {
                    plugin.getLogger().warning("Corrupt player data for " + id + ": " + e.getMessage() + " — starting fresh.");
                    return new YamlConfiguration();
                }
            }
            return new YamlConfiguration();
        });
    }

    public void markDirty(UUID uuid) {
        dirty.put(uuid, true);
        if (plugin.getConfigManager().getBoolean("storage.save-async", true)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> save(uuid));
        } else {
            save(uuid);
        }
    }

    private void save(UUID uuid) {
        YamlConfiguration doc = cache.get(uuid);
        if (doc == null) return;
        try {
            doc.save(fileFor(uuid));
            dirty.remove(uuid);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save data for " + uuid + ": " + e.getMessage());
        }
    }

    /** Saves (if dirty) then drops from memory — called on quit when storage.unload-on-quit is true. */
    public void unload(UUID uuid) {
        if (Boolean.TRUE.equals(dirty.get(uuid))) {
            save(uuid);
        }
        if (plugin.getConfigManager().getBoolean("storage.unload-on-quit", true)) {
            cache.remove(uuid);
        }
    }

    /** Synchronous save-everything, only used from onDisable(). */
    public void saveAllSync() {
        for (UUID uuid : dirty.keySet()) {
            save(uuid);
        }
    }
}
