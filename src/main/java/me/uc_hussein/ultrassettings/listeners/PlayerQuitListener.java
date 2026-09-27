package me.uc_hussein.ultrassettings.listeners;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Flushes and unloads a departing player's cached Ultras_settings data (not Ultras_TPA's — that plugin owns its own). */
public class PlayerQuitListener implements Listener {

    private final UltrasSettingsPlugin plugin;

    public PlayerQuitListener(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataStorage().unload(event.getPlayer().getUniqueId());
    }
}
