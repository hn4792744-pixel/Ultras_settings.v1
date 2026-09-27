package me.uc_hussein.ultrassettings.utils;

import me.uc_hussein.ultrassettings.models.VisualState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public final class ItemBuilder {

    private ItemBuilder() {}

    public static ItemStack build(VisualState state, PlaceholderUtil placeholders) {
        Material material = parseMaterial(state.material);
        ItemStack item = new ItemStack(material);
        applyMeta(item, state, placeholders);
        return item;
    }

    /** Same as build(), but forces a player skull with the given owner's skin. */
    public static ItemStack buildSkull(OfflinePlayer owner, VisualState state, PlaceholderUtil placeholders) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
            applyCommonMeta(skullMeta, state, placeholders);
            item.setItemMeta(skullMeta);
        }
        return item;
    }

    public static ItemStack decoration(String materialName, String name) {
        Material material = parseMaterial(materialName);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.render(name == null ? " " : name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void applyMeta(ItemStack item, VisualState state, PlaceholderUtil placeholders) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        applyCommonMeta(meta, state, placeholders);
        item.setItemMeta(meta);
    }

    private static void applyCommonMeta(ItemMeta meta, VisualState state, PlaceholderUtil placeholders) {
        String name = placeholders != null ? placeholders.apply(state.name) : state.name;
        meta.displayName(ColorUtil.render(name));

        List<String> lore = state.lore;
        if (lore != null && !lore.isEmpty()) {
            List<String> rendered = placeholders != null ? placeholders.applyAll(lore) : lore;
            meta.lore(rendered.stream().map(ColorUtil::render).toList());
        }

        if (state.customModelData > 0) {
            meta.setCustomModelData(state.customModelData);
        }
        if (state.glow) {
            // Paper's proper non-deprecated glow API (since 1.20.5) — no fake enchantment needed.
            meta.setEnchantmentGlintOverride(true);
        }
    }

    private static Material parseMaterial(String name) {
        if (name == null) return Material.STONE;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
}
