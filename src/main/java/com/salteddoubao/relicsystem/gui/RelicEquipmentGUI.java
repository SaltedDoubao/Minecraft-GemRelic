package com.salteddoubao.relicsystem.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.*;
import com.salteddoubao.relicsystem.util.RelicDisplayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 装备管理页面 - 显示当前穿戴的圣遗物
 */
public class RelicEquipmentGUI {
    public static final String TITLE = "§a§l圣遗物装备";
    private final MinecraftRelicSystem plugin;
    
    // 5个部位的固定位置
    private static final int FLOWER_SLOT = 10;
    private static final int PLUME_SLOT = 12;
    private static final int SANDS_SLOT = 14;
    private static final int GOBLET_SLOT = 28;
    private static final int CIRCLET_SLOT = 34;
    
    private static final int BACK_SLOT = 40;  // 返回主菜单按钮
    
    public RelicEquipmentGUI(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player player) {
        PlayerRelicProfile profile = plugin.getRelicProfileManager().get(player);
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 45, Component.text(TITLE));
        holder.setInventory(inv);
        
        // 设置边框
        setupBorder(inv);
        
        // 设置5个部位
        setupEquippedSlots(inv, profile);
        
        // 返回按钮
        inv.setItem(BACK_SLOT, createItem(Material.BARRIER, Component.text("§c§l返回主菜单"), 
            List.of(Component.text("§7点击返回圣遗物系统主菜单"))));
            
        // 套装统计显示
        setupSetBonusInfo(inv, profile);
            
        player.openInventory(inv);
    }
    
    private void setupBorder(Inventory inv) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text("§7"), null);
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                if (i != BACK_SLOT) {  // 避免覆盖返回按钮
                    inv.setItem(i, border);
                }
            }
        }
    }
    
    private void setupEquippedSlots(Inventory inv, PlayerRelicProfile profile) {
        Map<RelicSlot, RelicData> equipped = profile.getEquipped();
        
        setupSlot(inv, FLOWER_SLOT, RelicSlot.FLOWER, equipped.get(RelicSlot.FLOWER));
        setupSlot(inv, PLUME_SLOT, RelicSlot.PLUME, equipped.get(RelicSlot.PLUME));
        setupSlot(inv, SANDS_SLOT, RelicSlot.SANDS, equipped.get(RelicSlot.SANDS));
        setupSlot(inv, GOBLET_SLOT, RelicSlot.GOBLET, equipped.get(RelicSlot.GOBLET));
        setupSlot(inv, CIRCLET_SLOT, RelicSlot.CIRCLET, equipped.get(RelicSlot.CIRCLET));
    }
    
    private void setupSlot(Inventory inv, int slot, RelicSlot relicSlot, RelicData relic) {
        if (relic != null) {
            inv.setItem(slot, createEquippedItem(relic));
        } else {
            inv.setItem(slot, createEmptySlot(relicSlot));
        }
    }
    
    private void setupSetBonusInfo(Inventory inv, PlayerRelicProfile profile) {
        // 在右侧显示套装加成信息
        ItemStack info = createItem(Material.BOOK, Component.text("§6§l套装加成"), getSetBonusLore(profile));
        inv.setItem(16, info);
    }
    
    private List<Component> getSetBonusLore(PlayerRelicProfile profile) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7当前套装/属性总览:"));
        lore.add(Component.text("§7"));
        
        Map<String, Integer> counts = new java.util.HashMap<>();
        for (RelicData relic : profile.getEquipped().values()) {
            counts.merge(relic.getSetId(), 1, Integer::sum);
        }
        
        if (counts.isEmpty()) {
            lore.add(Component.text("§8未装备任何圣遗物"));
        } else {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                RelicSet set = plugin.getRelicManager().getRelicSet(entry.getKey());
                String name = set != null ? set.getName() : entry.getKey();
                int count = entry.getValue();
                
                lore.add(Component.text("§e" + name + " [" + count + "/4]:"));
                
                // 两件套效果
                if (count >= 2) {
                    lore.add(Component.text("  §a两件套 §7[已激活]"));
                    if (set != null) {
                        for (String desc : set.getTwoPieceEffects()) {
                            lore.add(Component.text("    §a- " + desc));
                        }
                    }
                } else {
                    lore.add(Component.text("  §8两件套 §7[未激活]"));
                    if (set != null) {
                        for (String desc : set.getTwoPieceEffects()) {
                            lore.add(Component.text("    §8- " + desc));
                        }
                    }
                }
                
                // 四件套效果
                if (count >= 4) {
                    lore.add(Component.text("  §a四件套 §7[已激活]"));
                    if (set != null) {
                        for (String desc : set.getFourPieceEffects()) {
                            lore.add(Component.text("    §a- " + desc));
                        }
                    }
                } else {
                    lore.add(Component.text("  §8四件套 §7[未激活]"));
                    if (set != null) {
                        for (String desc : set.getFourPieceEffects()) {
                            lore.add(Component.text("    §8- " + desc));
                        }
                    }
                }
                
                lore.add(Component.text("§7"));
            }
        }

        // 追加：当前已装备圣遗物属性合计（关键信息）
        java.util.Map<com.salteddoubao.relicsystem.relic.RelicStatType, java.lang.Double> total = plugin.getStatAggregationService().aggregate(profile);
        if (total != null && !total.isEmpty()) {
            lore.add(Component.text("§6属性合计:"));
            for (java.util.Map.Entry<com.salteddoubao.relicsystem.relic.RelicStatType, java.lang.Double> e : total.entrySet()) {
                lore.add(Component.text("  §7- " + e.getKey().getDisplay() + ": §a" + String.format("%.1f", e.getValue()) + (e.getKey().isPercent() ? "%" : "")));
            }
        }
        
        return lore;
    }
    
    private ItemStack createEquippedItem(RelicData relic) {
        // 优先使用套装模板材质
        RelicSet setRef = plugin.getRelicManager().getRelicSet(relic.getSetId());
        Material material = (setRef != null && setRef.getTemplateMaterial() != null)
                ? setRef.getTemplateMaterial()
                : RelicDisplayUtils.getRarityMaterial(relic.getRarity());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String setName = setRef != null ? setRef.getName() : relic.getSetId();
            meta.displayName(Component.text("§a[已装备] " + RelicDisplayUtils.getRarityColor(relic.getRarity()) + setName)
                .decoration(TextDecoration.ITALIC, false));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7部位: §f" + RelicDisplayUtils.getSlotDisplayName(relic.getSlot())));
            lore.add(Component.text("§7等级: §f" + relic.getLevel()));
            lore.add(Component.text("§7"));
            lore.add(Component.text("§6主词条:"));
            lore.add(Component.text("  " + relic.getMainStat().getType().getDisplay() + ": §a" + 
                String.format("%.1f", relic.getMainStat().getValue()) + 
                (relic.getMainStat().getType().isPercent() ? "%" : "")));
            
            if (!relic.getSubstats().isEmpty()) {
                lore.add(Component.text("§7"));
                lore.add(Component.text("§6副词条:"));
                for (RelicSubstat substat : relic.getSubstats()) {
                    lore.add(Component.text("  " + substat.getType().getDisplay() + ": §a" + 
                        String.format("%.1f", substat.getValue()) + 
                        (substat.getType().isPercent() ? "%" : "")));
                }
            }
            
            lore.add(Component.text("§7"));
            if (relic.isLocked()) {
                lore.add(Component.text("§c🔒 已锁定"));
            }
            lore.add(Component.text("§c点击卸下到仓库"));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createEmptySlot(RelicSlot slot) {
        Material material = RelicDisplayUtils.getSlotMaterial(slot);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text("§7" + RelicDisplayUtils.getSlotDisplayName(slot) + " §8[空]")
                .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("§7此部位未装备圣遗物"), Component.text("§7前往仓库选择圣遗物装备")));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            if (lore != null) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    
    // 获取槽位索引的方法
    public static int getFlowerSlot() { return FLOWER_SLOT; }
    public static int getPlumeSlot() { return PLUME_SLOT; }
    public static int getSandsSlot() { return SANDS_SLOT; }
    public static int getGobletSlot() { return GOBLET_SLOT; }
    public static int getCircletSlot() { return CIRCLET_SLOT; }
    public static int getBackSlot() { return BACK_SLOT; }

    public static class Holder implements InventoryHolder {
        private Inventory inventory;
        @Override
        public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
    }
}
