package me.uc_hussein.ultrassettings.services;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import org.bukkit.entity.Player;

/**
 * "/setting rest" and "/setting_admin rest <player>" both land here. Since
 * Ultras_TPA only exposes flip-toggles (not "set to X" commands), resetting
 * means comparing the real current state to config.yml's defaults and only
 * dispatching the toggle command for whichever ones actually differ.
 */
public class SettingsService {

    private static final String[] TOGGLE_KEYS = {"tpa", "tpahere", "tpaall", "auto"};

    private final UltrasSettingsPlugin plugin;

    public SettingsService(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @param actor the player the reset is performed AS (must be online — see TpaIntegration's
     *              class comment on why commands can only be dispatched for an online player)
     */
    public int resetToDefaults(Player actor) {
        if (!plugin.getTpaIntegration().isAvailable()) return 0;

        int changed = 0;
        for (String key : TOGGLE_KEYS) {
            boolean current = plugin.getTpaIntegration().readToggle(actor.getUniqueId(), key);
            boolean target = plugin.getConfigManager().getDefault(key);
            if (current != target) {
                String command = plugin.getTpaIntegration().getCommand(commandKeyFor(key));
                plugin.getTpaIntegration().dispatchAsPlayer(actor, command, null);
                changed++;
            }
        }
        return changed;
    }

    private String commandKeyFor(String toggleSource) {
        return switch (toggleSource) {
            case "tpa" -> "toggle";
            case "tpahere" -> "togglehere";
            case "tpaall" -> "toggleall";
            case "auto" -> "auto";
            default -> toggleSource;
        };
    }
}
