package me.uc_hussein.ultrassettings;

import me.uc_hussein.ultrassettings.commands.SettingAdminCommand;
import me.uc_hussein.ultrassettings.commands.SettingAdminTabCompleter;
import me.uc_hussein.ultrassettings.commands.SettingCommand;
import me.uc_hussein.ultrassettings.commands.SettingTabCompleter;
import me.uc_hussein.ultrassettings.config.ConfigManager;
import me.uc_hussein.ultrassettings.gui.GuiRegistry;
import me.uc_hussein.ultrassettings.integration.TpaIntegration;
import me.uc_hussein.ultrassettings.language.LanguageManager;
import me.uc_hussein.ultrassettings.listeners.InventoryListener;
import me.uc_hussein.ultrassettings.listeners.PlayerQuitListener;
import me.uc_hussein.ultrassettings.services.GuiOpener;
import me.uc_hussein.ultrassettings.services.SettingsService;
import me.uc_hussein.ultrassettings.services.SoundService;
import me.uc_hussein.ultrassettings.storage.PlayerDataStorage;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ultras_settings bootstrap. This plugin owns NO TPA logic — every feature
 * button just executes one of Ultras_TPA's own commands (see integration/TpaIntegration).
 * See onEnable() for the exact lifecycle order the spec asks for (section 65).
 */
public final class UltrasSettingsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private SoundService soundService;
    private TpaIntegration tpaIntegration;
    private GuiRegistry guiRegistry;
    private GuiOpener guiOpener;
    private SettingsService settingsService;
    private PlayerDataStorage playerDataStorage;

    @Override
    public void onEnable() {
        // Load Config
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // Load Languages
        this.languageManager = new LanguageManager(this);

        // Initialize Storage
        this.playerDataStorage = new PlayerDataStorage(this);

        // Services that don't depend on GUI files
        this.soundService = new SoundService(this);
        this.tpaIntegration = new TpaIntegration(this);
        this.settingsService = new SettingsService(this);

        // Load GUI files
        this.guiRegistry = new GuiRegistry(this);
        this.guiOpener = new GuiOpener(this);

        // Register Commands, TabCompleters, Listeners
        registerCommands();
        registerListeners();

        // Check Ultras_TPA
        checkIntegration();

        getLogger().info("Ultras_settings enabled (" + getDescription().getVersion() + ").");
    }

    @Override
    public void onDisable() {
        if (playerDataStorage != null) {
            playerDataStorage.saveAllSync();
        }
        getLogger().info("Ultras_settings disabled.");
    }

    private void checkIntegration() {
        if (org.bukkit.Bukkit.getPluginManager().getPlugin(configManager.getIntegrationPluginName()) == null) {
            getLogger().warning("'" + configManager.getIntegrationPluginName() + "' was not found. "
                    + "Ultras_settings will still load and its GUIs will still open, but every TPA-related "
                    + "button will show the \"not installed\" message until it's installed and enabled.");
        }
    }

    private void registerCommands() {
        registerCommand("setting", new SettingCommand(this), new SettingTabCompleter());
        registerCommand("setting_admin", new SettingAdminCommand(this), new SettingAdminTabCompleter(this));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter completer) {
        org.bukkit.command.PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml — skipping registration.");
            return;
        }
        command.setExecutor(executor);
        if (completer != null) command.setTabCompleter(completer);
    }

    private void registerListeners() {
        org.bukkit.event.HandlerList.unregisterAll(this); // safe on first call too; matters on reload
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
    }

    /** "/setting_admin reload" — config, both language files, and every GUI file, no restart needed. */
    public void reloadEverything() {
        reloadConfig();
        languageManager.reload();
        guiRegistry.load();
        registerListeners();
    }

    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public SoundService getSoundService() { return soundService; }
    public TpaIntegration getTpaIntegration() { return tpaIntegration; }
    public GuiRegistry getGuiRegistry() { return guiRegistry; }
    public GuiOpener getGuiOpener() { return guiOpener; }
    public SettingsService getSettingsService() { return settingsService; }
    public PlayerDataStorage getPlayerDataStorage() { return playerDataStorage; }
}
