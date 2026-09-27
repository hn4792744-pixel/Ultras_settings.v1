package me.uc_hussein.ultrassettings.services;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Resolves a sound reference two ways, tried in order: (1) a literal Bukkit Sound
 * enum name (used by per-button on/off sounds), (2) a key under config.yml's
 * "sounds:" section (used by shared system sounds like "back", "next-page").
 * Unknown/invalid values are logged once and skipped — never thrown.
 */
public class SoundService {

    private final UltrasSettingsPlugin plugin;

    public SoundService(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
    }

    public void play(Player player, String soundNameOrKey) {
        if (soundNameOrKey == null || soundNameOrKey.isBlank()) return;
        if (!plugin.getConfigManager().getBoolean("sounds.enabled", true)) return;

        if (tryLiteral(player, soundNameOrKey, 0.5f, 1.0f)) return;
        playSystemKey(player, soundNameOrKey);
    }

    public void play(Player player, String soundName, double volume, double pitch) {
        if (soundName == null || soundName.isBlank()) return;
        if (!plugin.getConfigManager().getBoolean("sounds.enabled", true)) return;
        if (tryLiteral(player, soundName, (float) volume, (float) pitch)) return;
        playSystemKey(player, soundName);
    }

    private boolean tryLiteral(Player player, String name, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(name);
            player.playSound(player.getLocation(), sound, volume, pitch);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void playSystemKey(Player player, String key) {
        String path = "sounds." + key;
        String soundName = plugin.getConfigManager().getString(path + ".sound", null);
        if (soundName == null) {
            plugin.getLogger().warning("Unknown sound reference '" + key + "' — not a Sound name and not a sounds." + key + " config key.");
            return;
        }
        float volume = (float) plugin.getConfigManager().getDouble(path + ".volume", 0.5);
        float pitch = (float) plugin.getConfigManager().getDouble(path + ".pitch", 1.0);
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound '" + soundName + "' for sounds." + key + " in config.yml.");
        }
    }
}
