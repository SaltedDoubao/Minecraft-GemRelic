package com.salteddoubao.relicsystem.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.*;

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
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        
        // 设置边框
        setupBorder(inv);
        
        // 设置5个部位
        setupEquippedSlots(inv, profile);
        
        // 返回按钮
        inv.setItem(BACK_SLOT, createItem(Material.BARRIER, "§c§l返回主菜单", 
            List.of("§7点击返回圣遗物系统主菜单")));
            
        // 套装统计显示
        setupSetBonusInfo(inv, profile);
            
        player.openInventory(inv);
    }
    
    private void setupBorder(Inventory inv) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
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
        ItemStack info = createItem(Material.BOOK, "§6§l套装加成", getSetBonusLore(profile));
        inv.setItem(16, info);
    }
    
    private List<String> getSetBonusLore(PlayerRelicProfile profile) {
        List<String> lore = new ArrayList<>();
        lore.add("§7当前套装件数:");
        lore.add("§7");
        
        Map<String, Integer> counts = new java.util.HashMap<>();
        for (RelicData relic : profile.getEquipped().values()) {
            counts.merge(relic.getSetId(), 1, Integer::sum);
        }
        
        if (counts.isEmpty()) {
            lore.add("§8未装备任何圣遗物");
        } else {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                RelicSet set = plugin.getRelicManager().getRelicSet(entry.getKey());
                String name = set != null ? set.getName() : entry.getKey();
                int count = entry.getValue();
                
                lore.add("§e" + name + " [" + count + "/4]:");
                
                // 两件套效果
                if (count >= 2) {
                    lore.add("  §a两件套 §7[已激活]");
                    if (set != null) {
                        for (String desc : set.getTwoPieceEffects()) {
                            lore.add("    §a- " + desc);
                        }
                    }
                } else {
                    lore.add("  §8两件套 §7[未激活]");
                    if (set != null) {
                        for (String desc : set.getTwoPieceEffects()) {
                            lore.add("    §8- " + desc);
                        }
                    }
                }
                
                // 四件套效果
                if (count >= 4) {
                    lore.add("  §a四件套 §7[已激活]");
                    if (set != null) {
                        for (String desc : set.getFourPieceEffects()) {
                            lore.add("    §a- " + desc);
                        }
                    }
                } else {
                    lore.add("  §8四件套 §7[未激活]");
                    if (set != null) {
                        for (String desc : set.getFourPieceEffects()) {
                            lore.add("    §8- " + desc);
                        }
                    }
                }
                
                lore.add("§7");
            }
        }
        
        return lore;
    }
    
    private ItemStack createEquippedItem(RelicData relic) {
        Material material = getRarityMaterial(relic.getRarity());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            RelicSet set = plugin.getRelicManager().getRelicSet(relic.getSetId());
            String setName = set != null ? set.getName() : relic.getSetId();
            meta.setDisplayName("§a[已装备] " + getRarityColor(relic.getRarity()) + setName);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7部位: §f" + getSlotDisplayName(relic.getSlot()));
            lore.add("§7等级: §f" + relic.getLevel());
            lore.add("§7");
            lore.add("§6主词条:");
            lore.add("  " + relic.getMainStat().getType().getDisplay() + ": §a" + 
                String.format("%.1f", relic.getMainStat().getValue()) + 
                (relic.getMainStat().getType().isPercent() ? "%" : ""));
            
            if (!relic.getSubstats().isEmpty()) {
                lore.add("§7");
                lore.add("§6副词条:");
                for (RelicSubstat substat : relic.getSubstats()) {
                    lore.add("  " + substat.getType().getDisplay() + ": §a" + 
                        String.format("%.1f", substat.getValue()) + 
                        (substat.getType().isPercent() ? "%" : ""));
                }
            }
            
            lore.add("§7");
            if (relic.isLocked()) {
                lore.add("§c🔒 已锁定");
            }
            lore.add("§c点击卸下到仓库");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createEmptySlot(RelicSlot slot) {
        Material material = getSlotMaterial(slot);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§7" + getSlotDisplayName(slot) + " §8[空]");
            meta.setLore(List.of("§7此部位未装备圣遗物", "§7前往仓库选择圣遗物装备"));
            item.setItemMeta(meta);
        }
        
        return item;
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
    
    private Material getRarityMaterial(RelicRarity rarity) {
        return switch (rarity) {
            case WHITE -> Material.QUARTZ;
            case GREEN -> Material.EMERALD;
            case BLUE -> Material.LAPIS_LAZULI;
            case PURPLE -> Material.AMETHYST_SHARD;
            case GOLD -> Material.GOLD_INGOT;
        };
    }
    
    private String getRarityColor(RelicRarity rarity) {
        return switch (rarity) {
            case WHITE -> "§f";
            case GREEN -> "§a";
            case BLUE -> "§9";
            case PURPLE -> "§d";
            case GOLD -> "§6";
        };
    }
    
    private Material getSlotMaterial(RelicSlot slot) {
        return switch (slot) {
            case FLOWER -> Material.BLUE_ORCHID;
            case PLUME -> Material.FEATHER;
            case SANDS -> Material.CLOCK;
            case GOBLET -> Material.POTION;
            case CIRCLET -> Material.PLAYER_HEAD;
        };
    }
    
    private String getSlotDisplayName(RelicSlot slot) {
        return switch (slot) {
            case FLOWER -> "生之花";
            case PLUME -> "死之羽";
            case SANDS -> "时之沙";
            case GOBLET -> "空之杯";
            case CIRCLET -> "理之冠";
        };
    }
    
    // 获取槽位索引的方法
    public static int getFlowerSlot() { return FLOWER_SLOT; }
    public static int getPlumeSlot() { return PLUME_SLOT; }
    public static int getSandsSlot() { return SANDS_SLOT; }
    public static int getGobletSlot() { return GOBLET_SLOT; }
    public static int getCircletSlot() { return CIRCLET_SLOT; }
    public static int getBackSlot() { return BACK_SLOT; }
}
