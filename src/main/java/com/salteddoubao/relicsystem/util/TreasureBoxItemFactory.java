package com.salteddoubao.relicsystem.util;

import java.nio.charset.StandardCharsets;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;

public class TreasureBoxItemFactory {
    private final MinecraftRelicSystem plugin;
    private final NamespacedKey key;

    public TreasureBoxItemFactory(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "treasure_box_id");
    }

    public ItemStack create(String boxId, String displayName, java.util.List<String> lore, String material, Integer cmd, boolean glow) {
        Material mat;
        try { mat = Material.valueOf(material); } catch (Exception e) { mat = Material.CHEST; }
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (displayName != null) meta.setDisplayName(displayName);
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        if (cmd != null) meta.setCustomModelData(cmd);
        if (glow) {
            try {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } catch (Throwable ignore) {}
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, boxId);
        item.setItemMeta(meta);
        return item;
    }

    public String readBoxId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String v = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return v;
    }
}


