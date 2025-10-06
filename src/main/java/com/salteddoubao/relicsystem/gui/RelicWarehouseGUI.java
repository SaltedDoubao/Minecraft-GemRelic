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

/**
 * 圣遗物仓库页面 - 管理仓库中的圣遗物（取出、装备、放入）
 */
public class RelicWarehouseGUI {
    public static final String TITLE_PREFIX = "§b§l圣遗物仓库";
    private final MinecraftRelicSystem plugin;
    
    // 界面布局
    private static final int[] WAREHOUSE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 
                                                  19, 20, 21, 22, 23, 24, 25,
                                                  28, 29, 30, 31, 32, 33, 34}; // 仓库显示区域（21格）
    private static final int PREV_PAGE_SLOT = 39;
    private static final int NEXT_PAGE_SLOT = 41;
    private static final int FILTER_SLOT = 40;
    private static final int BACK_SLOT = 36;  // 返回主菜单
    private static final int PUT_IN_SLOT = 44;  // 从背包放入仓库按钮

    public RelicWarehouseGUI(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, RelicSlot filterSlot, int page) {
        PlayerRelicProfile profile = plugin.getRelicProfileManager().get(player);
        
        String title = TITLE_PREFIX;
        if (filterSlot != null) {
            title += " - " + getSlotDisplayName(filterSlot);
        }
        title += " (" + (page + 1) + ")";
        
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        // 设置边框
        setupBorder(inv);
        
        // 设置仓库分页显示
        setupWarehouseDisplay(inv, profile, filterSlot, page);
        
        // 设置控制按钮
        setupControls(inv, filterSlot, page, profile);
        
        player.openInventory(inv);
    }

    private void setupBorder(Inventory inv) {
        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
        for (int i = 0; i < 54; i++) {
            boolean isBorder = (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8);
            boolean isControlSlot = (i == PREV_PAGE_SLOT || i == NEXT_PAGE_SLOT || i == FILTER_SLOT || 
                                   i == BACK_SLOT || i == PUT_IN_SLOT);
            boolean isWarehouseSlot = false;
            for (int w : WAREHOUSE_SLOTS) {
                if (i == w) {
                    isWarehouseSlot = true;
                    break;
                }
            }
            
            if (isBorder && !isControlSlot && !isWarehouseSlot) {
                inv.setItem(i, border);
            }
        }
    }

    private void setupWarehouseDisplay(Inventory inv, PlayerRelicProfile profile, RelicSlot filterSlot, int page) {
        List<RelicData> items = filterSlot != null ? 
            profile.getWarehouseBySlot(filterSlot) : 
            profile.getWarehouse();
        
        // 如果仓库为空，显示提示物品
        if (items.isEmpty() && page == 0) {
            ItemStack hint = createGuiItem(Material.BARRIER, "§c§l仓库为空", 
                List.of("§7仓库中没有圣遗物", "§7", 
                       "§e获取圣遗物的方法:", 
                       "§71. 执行 §e/relic test §7获取测试圣遗物",
                       "§72. 将圣遗物物品放入背包后使用 §e放入按钮",
                       "§73. 在背包中 §eShift-点击 §7圣遗物快速放入"));
            inv.setItem(WAREHOUSE_SLOTS[10], hint); // 中央位置显示提示
            return;
        }
        
        int startIndex = page * WAREHOUSE_SLOTS.length;
        int endIndex = Math.min(startIndex + WAREHOUSE_SLOTS.length, items.size());
        
        for (int i = 0; i < WAREHOUSE_SLOTS.length; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < endIndex) {
                RelicData relic = items.get(itemIndex);
                inv.setItem(WAREHOUSE_SLOTS[i], createWarehouseItem(relic));
            }
        }
    }

    private void setupControls(Inventory inv, RelicSlot filterSlot, int page, PlayerRelicProfile profile) {
        // 上一页按钮
        if (page > 0) {
            inv.setItem(PREV_PAGE_SLOT, createGuiItem(Material.ARROW, "§a上一页", 
                List.of("§7点击查看上一页")));
        }
        
        // 下一页按钮
        List<RelicData> items = filterSlot != null ? profile.getWarehouseBySlot(filterSlot) : profile.getWarehouse();
        int maxPage = Math.max(0, (items.size() - 1) / WAREHOUSE_SLOTS.length);
        if (page < maxPage) {
            inv.setItem(NEXT_PAGE_SLOT, createGuiItem(Material.ARROW, "§a下一页", 
                List.of("§7点击查看下一页")));
        }
        
        // 筛选按钮
        String filterText = filterSlot != null ? getSlotDisplayName(filterSlot) : "全部";
        inv.setItem(FILTER_SLOT, createGuiItem(Material.HOPPER, "§e筛选: " + filterText, 
            List.of("§7点击切换筛选条件", "§7当前筛选: " + filterText)));
            
        // 返回主菜单按钮
        inv.setItem(BACK_SLOT, createGuiItem(Material.BARRIER, "§c§l返回主菜单", 
            List.of("§7点击返回圣遗物系统主菜单")));
            
        // 从背包放入按钮
        inv.setItem(PUT_IN_SLOT, createGuiItem(Material.HOPPER, "§6§l放入圣遗物", 
            List.of("§7将背包中的圣遗物放入仓库", "§7右键点击此按钮执行操作")));
    }

    private ItemStack createWarehouseItem(RelicData relic) {
        Material material = getRarityMaterial(relic.getRarity());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            RelicSet set = plugin.getRelicManager().getRelicSet(relic.getSetId());
            String setName = set != null ? set.getName() : relic.getSetId();
            meta.setDisplayName(getRarityColor(relic.getRarity()) + setName + " - " + getSlotDisplayName(relic.getSlot()));
            
            List<String> lore = new ArrayList<>();
            lore.add("§7等级: §f" + relic.getLevel());
            lore.add("§7稀有度: " + getRarityColor(relic.getRarity()) + relic.getRarity().getStars() + "★");
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
            lore.add("§7");
            lore.add("§e左键：装备到对应部位");
            lore.add("§e右键：取出到背包");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createEmptySlotItem(RelicSlot slot) {
        Material material = getSlotMaterial(slot);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§7" + getSlotDisplayName(slot) + " §8[空]");
            meta.setLore(List.of("§7此部位未装备圣遗物", "§e点击从仓库选择"));
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createGuiItem(Material material, String name, List<String> lore) {
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

    // 获取槽位索引相关方法  
    public static int[] getWarehouseSlots() { return WAREHOUSE_SLOTS; }
    public static int getPrevPageSlot() { return PREV_PAGE_SLOT; }
    public static int getNextPageSlot() { return NEXT_PAGE_SLOT; }
    public static int getFilterSlot() { return FILTER_SLOT; }
    public static int getBackSlot() { return BACK_SLOT; }
    public static int getPutInSlot() { return PUT_IN_SLOT; }
}
