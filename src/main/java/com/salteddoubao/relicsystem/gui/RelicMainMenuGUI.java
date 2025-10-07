package com.salteddoubao.relicsystem.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;

import java.util.List;

/**
 * 圣遗物系统主菜单
 */
public class RelicMainMenuGUI {
    public static final String TITLE = "§6§l圣遗物系统";
    private final MinecraftRelicSystem plugin;
    
    // 按钮位置
    private static final int EQUIP_SLOT = 20;  // 装备管理
    private static final int WAREHOUSE_SLOT = 24;  // 仓库管理
    
    public RelicMainMenuGUI(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 45, TITLE);
        holder.setInventory(inv);
        
        // 设置边框
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }
        
        // 装备管理按钮
        inv.setItem(EQUIP_SLOT, createItem(Material.DIAMOND_CHESTPLATE, 
            "§a§l装备管理", 
            List.of("§7查看和管理当前装备的圣遗物", "§7可以卸下圣遗物到仓库", "§e点击进入")));
            
        // 仓库管理按钮
        inv.setItem(WAREHOUSE_SLOT, createItem(Material.CHEST, 
            "§b§l仓库管理", 
            List.of("§7查看仓库中的圣遗物", "§7可以装备、取出到背包", "§e点击进入")));
            
        player.openInventory(inv);
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public static int getEquipSlot() { return EQUIP_SLOT; }
    public static int getWarehouseSlot() { return WAREHOUSE_SLOT; }

    public static class Holder implements InventoryHolder {
        private Inventory inventory;
        @Override
        public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
    }
}
