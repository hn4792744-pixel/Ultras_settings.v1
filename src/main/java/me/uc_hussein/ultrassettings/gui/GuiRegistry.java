package me.uc_hussein.ultrassettings.gui;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import me.uc_hussein.ultrassettings.models.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Auto-discovers every *.yml file under plugins/Ultras_settings/gui/ — dropping a
 * new file there and running /setting_admin reload makes it available immediately,
 * with zero Java changes (spec sections 18/24/25/45).
 */
public class GuiRegistry {

    private final UltrasSettingsPlugin plugin;
    private final File folder;
    private final Map<String, GuiDefinition> definitions = new LinkedHashMap<>();

    public GuiRegistry(UltrasSettingsPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "gui");
        if (!folder.exists()) {
            folder.mkdirs();
            for (String name : new String[]{"setting.gui.yml", "setting.gui1.yml", "setting.gui2.yml",
                    "blocklist.gui.yml", "block.gui.yml"}) {
                plugin.saveResource("gui/" + name, false);
            }
        }
        load();
    }

    public void load() {
        definitions.clear();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4);
            try {
                GuiDefinition def = parse(id, file);
                definitions.put(id.toLowerCase(), def);
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("[debug] Loaded GUI '" + id + "' (" + def.buttons.size() + " buttons).");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load GUI file '" + file.getName() + "': " + e.getMessage()
                        + " — this GUI will be unavailable until the file is fixed. Other GUIs are unaffected.");
            }
        }
    }

    public boolean exists(String id) {
        return definitions.containsKey(id.toLowerCase());
    }

    public GuiDefinition get(String id) {
        return definitions.get(id.toLowerCase());
    }

    public java.util.List<String> allIds() {
        return new java.util.ArrayList<>(definitions.keySet());
    }

    // -----------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------

    private GuiDefinition parse(String id, File file) {
        YamlConfiguration doc = YamlConfiguration.loadConfiguration(file);
        GuiDefinition def = new GuiDefinition();
        def.id = id;
        def.title = doc.getString("title", "Menu");
        def.size = clampSize(doc.getInt("size", 27));
        def.closeOnAction = doc.getBoolean("close-on-action", false);
        def.allowPlayerItems = doc.getBoolean("allow-player-items", false);
        def.soundOpen = doc.getString("sound-open", null);

        ConfigurationSection deco = doc.getConfigurationSection("decoration");
        if (deco != null) {
            GuiDefinition.DecorationSpec spec = new GuiDefinition.DecorationSpec();
            spec.material = deco.getString("material", "GRAY_STAINED_GLASS_PANE");
            spec.name = deco.getString("name", " ");
            spec.slots = deco.getIntegerList("slots");
            def.decoration = spec;
        }

        def.playersOnlineOnly = doc.getBoolean("players.online-only", true);
        def.playerSlots = doc.getIntegerList("player-slots");

        ConfigurationSection headTemplate = doc.getConfigurationSection("player-head");
        if (headTemplate != null) {
            VisualState state = new VisualState();
            state.name = headTemplate.getString("name", "%player%");
            state.lore = headTemplate.getStringList("lore");
            def.playerHeadTemplate = state;
            def.playerHeadSlot = headTemplate.getInt("slot", -1);
        }

        ConfigurationSection noPlayers = doc.getConfigurationSection("no-players");
        if (noPlayers != null) {
            GuiDefinition.NoPlayersSpec spec = new GuiDefinition.NoPlayersSpec();
            spec.slot = noPlayers.getInt("slot", 22);
            spec.material = noPlayers.getString("material", "BARRIER");
            spec.name = noPlayers.getString("name", "NO PLAYERS");
            spec.lore = noPlayers.getStringList("lore");
            def.noPlayers = spec;
        }

        ConfigurationSection buttons = doc.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String key : buttons.getKeys(false)) {
                ConfigurationSection b = buttons.getConfigurationSection(key);
                if (b == null) continue;
                try {
                    ButtonDefinition button = parseButton(key, b);
                    if (button.enabled) {
                        def.buttons.put(key, button);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Skipping invalid button '" + key + "' in '" + file.getName()
                            + "': " + e.getMessage());
                }
            }
        }

        validateSlots(id, def);
        return def;
    }

    private ButtonDefinition parseButton(String id, ConfigurationSection section) {
        ButtonDefinition button = new ButtonDefinition();
        button.id = id;
        button.slot = section.getInt("slot", -1);
        button.enabled = section.getBoolean("enabled", true);
        button.permission = section.getString("permission", null);

        String typeName = section.getString("type", "ACTION");
        button.type = ButtonType.valueOf(typeName.toUpperCase());

        switch (button.type) {
            case TOGGLE -> {
                button.toggleSource = section.getString("toggle-source", null);
                button.on = parseState(section.getConfigurationSection("on"));
                button.off = parseState(section.getConfigurationSection("off"));
            }
            case BLOCK_STATUS, UNBLOCK_STATUS -> {
                button.active = parseState(section.getConfigurationSection("active"));
                button.inactive = parseState(section.getConfigurationSection("inactive"));
            }
            case ACTION -> {
                button.single = parseState(section);
                String actionName = section.getString("action", "NONE");
                button.action = ActionType.valueOf(actionName.toUpperCase());
                button.targetGui = section.getString("target-gui", null);
            }
            case COMING_SOON -> button.single = parseState(section);
        }
        return button;
    }

    private VisualState parseState(ConfigurationSection section) {
        VisualState state = new VisualState();
        if (section == null) return state;
        state.material = section.getString("material", "STONE");
        state.name = section.getString("name", "");
        state.lore = section.getStringList("lore");
        state.sound = section.getString("sound", null);
        state.volume = section.getDouble("volume", 0.5);
        state.pitch = section.getDouble("pitch", 1.0);
        state.glow = section.getBoolean("glow", false);
        state.customModelData = section.getInt("custom-model-data", 0);
        state.command = section.getString("command", null);
        state.message = section.getString("message", null);
        return state;
    }

    private int clampSize(int size) {
        int rounded = Math.max(9, Math.min(54, size));
        return rounded - (rounded % 9 == 0 ? 0 : rounded % 9);
    }

    private void validateSlots(String guiId, GuiDefinition def) {
        for (ButtonDefinition button : def.buttons.values()) {
            if (button.slot < 0 || button.slot >= def.size) {
                plugin.getLogger().warning("Button '" + button.id + "' in '" + guiId
                        + ".yml' has slot " + button.slot + " outside the GUI's size (" + def.size + ") — it will not be shown.");
                button.enabled = false;
            }
        }
        def.buttons.values().removeIf(b -> !b.enabled);
    }
}
