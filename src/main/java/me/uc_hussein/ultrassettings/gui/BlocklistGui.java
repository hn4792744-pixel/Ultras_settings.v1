package me.uc_hussein.ultrassettings.gui;

import me.uc_hussein.ultrassettings.UltrasSettingsPlugin;
import me.uc_hussein.ultrassettings.models.ButtonDefinition;
import me.uc_hussein.ultrassettings.models.GuiDefinition;
import me.uc_hussein.ultrassettings.models.VisualState;
import me.uc_hussein.ultrassettings.utils.ColorUtil;
import me.uc_hussein.ultrassettings.utils.ItemBuilder;
import me.uc_hussein.ultrassettings.utils.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists online players so {@code settingsOwner}'s block list can be managed.
 * "settingsOwner" is whoever's settings are open (the viewer themselves for a
 * normal /setting, or the admin's target for /setting_admin <player>).
 */
public class BlocklistGui implements ClickableGui {

    private final UltrasSettingsPlugin plugin;
    private final Player viewer;
    private final OfflinePlayer settingsOwner;
    private final GuiDefinition def;
    private final Inventory inventory;
    private int page;

    private final Map<Integer, ButtonDefinition> actionSlots = new HashMap<>();
    private final Map<Integer, Player> playerSlotsRendered = new HashMap<>();

    public BlocklistGui(UltrasSettingsPlugin plugin, Player viewer, OfflinePlayer settingsOwner, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.settingsOwner = settingsOwner;
        this.page = page;
        this.def = plugin.getGuiRegistry().get("blocklist.gui");
        this.inventory = Bukkit.createInventory(this, def != null ? def.size : 45,
                ColorUtil.render(def != null ? def.title : "Block List"));
        render();
    }

    public void open() {
        viewer.openInventory(inventory);
        if (def != null && def.soundOpen != null) {
            plugin.getSoundService().play(viewer, def.soundOpen);
        }
    }

    private List<Player> eligiblePlayers() {
        List<Player> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(settingsOwner.getUniqueId())) list.add(p);
        }
        return list;
    }

    private void render() {
        if (def == null) return;
        inventory.clear();
        actionSlots.clear();
        playerSlotsRendered.clear();

        if (def.decoration != null) {
            ItemStack deco = ItemBuilder.decoration(def.decoration.material, def.decoration.name);
            for (int slot : def.decoration.slots) {
                if (slot >= 0 && slot < def.size) inventory.setItem(slot, deco);
            }
        }

        List<Player> players = eligiblePlayers();
        List<Integer> slots = def.playerSlots;
        int perPage = Math.max(1, slots.size());
        int totalPages = Math.max(1, (int) Math.ceil(players.size() / (double) perPage));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        if (players.isEmpty() && def.noPlayers != null) {
            ItemStack noPlayers = new ItemStack(parseMaterialSafe(def.noPlayers.material));
            var meta = noPlayers.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtil.render(def.noPlayers.name));
                if (!def.noPlayers.lore.isEmpty()) meta.lore(def.noPlayers.lore.stream().map(ColorUtil::render).toList());
                noPlayers.setItemMeta(meta);
            }
            if (def.noPlayers.slot >= 0 && def.noPlayers.slot < def.size) {
                inventory.setItem(def.noPlayers.slot, noPlayers);
            }
        } else {
            int from = page * perPage;
            int to = Math.min(players.size(), from + perPage);
            for (int i = from; i < to; i++) {
                Player target = players.get(i);
                int slot = slots.get(i - from);
                boolean blocked = plugin.getTpaIntegration().isBlocked(settingsOwner.getUniqueId(), target.getUniqueId());
                String status = plugin.getLanguageManager().raw(blocked ? "block.status-blocked" : "block.status-not-blocked");
                PlaceholderUtil placeholders = PlaceholderUtil.create().put("player", target.getName()).put("status", status);
                VisualState state = def.playerHeadTemplate != null ? def.playerHeadTemplate : new VisualState();
                ItemStack head = ItemBuilder.buildSkull(target, state, placeholders);
                inventory.setItem(slot, head);
                playerSlotsRendered.put(slot, target);
            }
        }

        for (ButtonDefinition button : def.buttons.values()) {
            if (button.type != me.uc_hussein.ultrassettings.models.ButtonType.ACTION) continue;
            if (button.action == me.uc_hussein.ultrassettings.models.ActionType.PREVIOUS_PAGE && page <= 0) continue;
            if (button.action == me.uc_hussein.ultrassettings.models.ActionType.NEXT_PAGE && page >= totalPages - 1) continue;
            if (button.slot < 0 || button.slot >= def.size) continue;
            inventory.setItem(button.slot, ItemBuilder.build(button.single, null));
            actionSlots.put(button.slot, button);
        }
    }

    private org.bukkit.Material parseMaterialSafe(String name) {
        try {
            return org.bukkit.Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return org.bukkit.Material.BARRIER;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event, Player clicker) {
        if (def == null) return;
        int slot = event.getRawSlot();

        ButtonDefinition action = actionSlots.get(slot);
        if (action != null) {
            VisualState state = action.single;
            if (state.sound != null) plugin.getSoundService().play(clicker, state.sound, state.volume, state.pitch);
            switch (action.action) {
                case BACK, OPEN_GUI -> plugin.getGuiOpener().open(viewer, settingsOwner, action.targetGui);
                case NEXT_PAGE -> { page++; render(); }
                case PREVIOUS_PAGE -> { page--; render(); }
                case CLOSE -> viewer.closeInventory();
                default -> {}
            }
            return;
        }

        Player target = playerSlotsRendered.get(slot);
        if (target != null) {
            new BlockGui(plugin, viewer, settingsOwner, target, this).open();
        }
    }
}
