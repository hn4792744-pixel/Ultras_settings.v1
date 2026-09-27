package me.uc_hussein.ultrassettings.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuiDefinition {

    public String id; // file name without ".yml", e.g. "setting.gui", "blocklist.gui"
    public String title = "Menu";
    public int size = 27;
    public boolean closeOnAction = false;
    public boolean allowPlayerItems = false;
    public String soundOpen; // resolved the same way as any button sound (literal Sound or a config key)

    public DecorationSpec decoration;

    /** Insertion order preserved so a full-grid layout renders left-to-right, top-to-bottom predictably. */
    public final Map<String, ButtonDefinition> buttons = new LinkedHashMap<>();

    // --- blocklist.gui.yml specifics (null/empty for every other GUI) ---
    public boolean playersOnlineOnly = true;
    public List<Integer> playerSlots = new ArrayList<>();
    public VisualState playerHeadTemplate; // name/lore use %player% %status% (blocklist) or %uuid% %online% (block)
    public int playerHeadSlot = -1;        // block.gui.yml: single fixed slot instead of a list
    public NoPlayersSpec noPlayers;

    public static class DecorationSpec {
        public String material = "GRAY_STAINED_GLASS_PANE";
        public String name = " ";
        public List<Integer> slots = new ArrayList<>();
    }

    public static class NoPlayersSpec {
        public int slot;
        public String material = "BARRIER";
        public String name = "";
        public List<String> lore = new ArrayList<>();
    }
}
