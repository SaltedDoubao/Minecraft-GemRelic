package com.lymc.gemrelic.listener;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.gui.*;
import com.lymc.gemrelic.relic.*;
import com.lymc.gemrelic.util.RelicItemConverter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelicGUIListener implements Listener {
    private final GemRelicPlugin plugin;
    private static final Pattern WAREHOUSE_TITLE = Pattern.compile("§b§l圣遗物仓库.*\\((\\d+)\\)");
    
    public RelicGUIListener(GemRelicPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        
        // 主菜单
        if (title.equals(RelicMainMenuGUI.TITLE)) {
            e.setCancelled(true);
            handleMainMenuClick(player, e);
            return;
        }
        
        // 装备页面
        if (title.equals(RelicEquipmentGUI.TITLE)) {
            e.setCancelled(true);
            handleEquipmentClick(player, e);
            return;
        }
        
        // 仓库界面
        if (title.startsWith(RelicWarehouseGUI.TITLE_PREFIX)) {
            e.setCancelled(true);
            handleWarehouseClick(player, e, title);
            return;
        }
    }
    
    private void handleMainMenuClick(Player player, InventoryClickEvent e) {
        int slot = e.getSlot();
        
        if (slot == RelicMainMenuGUI.getEquipSlot()) {
            // 打开装备页面
            new RelicEquipmentGUI(plugin).open(player);
        } else if (slot == RelicMainMenuGUI.getWarehouseSlot()) {
            // 打开仓库页面
            new RelicWarehouseGUI(plugin).open(player, null, 0);
        }
    }
    
    private void handleEquipmentClick(Player player, InventoryClickEvent e) {
        int slot = e.getSlot();
        PlayerRelicProfile profile = plugin.getRelicProfileManager().get(player);
        
        // 检查是否点击了返回按钮
        if (slot == RelicEquipmentGUI.getBackSlot()) {
            new RelicMainMenuGUI(plugin).open(player);
            return;
        }
        
        // 检查是否点击了装备槽位并卸下
        RelicSlot targetSlot = getEquipmentSlotType(slot);
        if (targetSlot != null) {
            RelicData equipped = profile.getEquipped().get(targetSlot);
            if (equipped != null) {
                profile.unequip(targetSlot);
                plugin.getRelicEffectService().refresh(player, profile);
                player.sendMessage("§a已卸下 " + getSlotDisplayName(targetSlot) + " 部位的圣遗物到仓库");
                // 刷新装备页面
                new RelicEquipmentGUI(plugin).open(player);
            }
        }
    }
    
    private RelicSlot getEquipmentSlotType(int slot) {
        if (slot == RelicEquipmentGUI.getFlowerSlot()) return RelicSlot.FLOWER;
        if (slot == RelicEquipmentGUI.getPlumeSlot()) return RelicSlot.PLUME;
        if (slot == RelicEquipmentGUI.getSandsSlot()) return RelicSlot.SANDS;
        if (slot == RelicEquipmentGUI.getGobletSlot()) return RelicSlot.GOBLET;
        if (slot == RelicEquipmentGUI.getCircletSlot()) return RelicSlot.CIRCLET;
        return null;
    }

    private void handleWarehouseClick(Player player, InventoryClickEvent e, String title) {
        int slot = e.getSlot();
        ClickType clickType = e.getClick();
        
        // 解析当前页数和筛选
        RelicSlot currentFilter = parseCurrentFilter(title);
        int currentPage = parseCurrentPage(title);
        
        PlayerRelicProfile profile = plugin.getRelicProfileManager().get(player);
        RelicItemConverter converter = plugin.getRelicItemConverter();
        
        // 返回主菜单
        if (slot == RelicWarehouseGUI.getBackSlot()) {
            new RelicMainMenuGUI(plugin).open(player);
            return;
        }
        
        // 从背包放入仓库
        if (slot == RelicWarehouseGUI.getPutInSlot()) {
            handlePutInFromInventory(player, profile, currentFilter, currentPage);
            return;
        }
        
        // 点击仓库物品
        if (isWarehouseSlot(slot)) {
            List<RelicData> items = currentFilter != null ? 
                profile.getWarehouseBySlot(currentFilter) : 
                profile.getWarehouse();
            
            int itemIndex = getWarehouseIndex(slot) + currentPage * RelicWarehouseGUI.getWarehouseSlots().length;
            if (itemIndex < items.size()) {
                RelicData relic = items.get(itemIndex);
                
                if (clickType == ClickType.LEFT) {
                    // 左键：装备
                    profile.equip(relic);
                    plugin.getRelicEffectService().refresh(player, profile);
                    player.sendMessage("§a已装备 " + getSlotDisplayName(relic.getSlot()) + ": " + 
                        plugin.getRelicManager().getRelicSet(relic.getSetId()).getName());
                } else if (clickType == ClickType.RIGHT) {
                    // 右键：取出到背包
                    ItemStack relicItem = converter.toItemStack(relic);
                    if (player.getInventory().firstEmpty() != -1) {
                        profile.removeFromWarehouse(relic);
                        player.getInventory().addItem(relicItem);
                        player.sendMessage("§a已取出圣遗物到背包");
                    } else {
                        player.sendMessage("§c背包已满，无法取出");
                    }
                }
                
                // 刷新仓库页面
                new RelicWarehouseGUI(plugin).open(player, currentFilter, currentPage);
            }
            return;
        }
        
        // 控制按钮
        if (slot == RelicWarehouseGUI.getPrevPageSlot() && currentPage > 0) {
            new RelicWarehouseGUI(plugin).open(player, currentFilter, currentPage - 1);
        } else if (slot == RelicWarehouseGUI.getNextPageSlot()) {
            new RelicWarehouseGUI(plugin).open(player, currentFilter, currentPage + 1);
        } else if (slot == RelicWarehouseGUI.getFilterSlot()) {
            // 循环切换筛选条件
            RelicSlot nextFilter = getNextFilter(currentFilter);
            new RelicWarehouseGUI(plugin).open(player, nextFilter, 0);
        }
    }
    
    private void handlePutInFromInventory(Player player, PlayerRelicProfile profile, RelicSlot currentFilter, int currentPage) {
        RelicItemConverter converter = plugin.getRelicItemConverter();
        int putInCount = 0;
        
        // 遍历背包查找圣遗物物品
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (converter.isRelicItem(item)) {
                RelicData relic = converter.fromItemStack(item);
                if (relic != null) {
                    // 移除背包中的物品并添加到仓库
                    player.getInventory().setItem(i, null);
                    profile.addToWarehouse(relic);
                    putInCount++;
                }
            }
        }
        
        if (putInCount > 0) {
            player.sendMessage("§a已将 " + putInCount + " 件圣遗物放入仓库");
            // 刷新仓库页面
            new RelicWarehouseGUI(plugin).open(player, currentFilter, currentPage);
        } else {
            player.sendMessage("§c背包中没有圣遗物物品");
        }
    }

    private RelicSlot parseCurrentFilter(String title) {
        for (RelicSlot slot : RelicSlot.values()) {
            if (title.contains(getSlotDisplayName(slot))) {
                return slot;
            }
        }
        return null;
    }

    private int parseCurrentPage(String title) {
        Matcher m = WAREHOUSE_TITLE.matcher(title);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1)) - 1; // 转换为0开始的页数
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private boolean isWarehouseSlot(int slot) {
        for (int warehouse : RelicWarehouseGUI.getWarehouseSlots()) {
            if (slot == warehouse) return true;
        }
        return false;
    }

    private int getWarehouseIndex(int slot) {
        int[] warehouse = RelicWarehouseGUI.getWarehouseSlots();
        for (int i = 0; i < warehouse.length; i++) {
            if (warehouse[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private RelicSlot getNextFilter(RelicSlot current) {
        if (current == null) return RelicSlot.FLOWER;
        RelicSlot[] slots = RelicSlot.values();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == current) {
                return i == slots.length - 1 ? null : slots[i + 1];
            }
        }
        return null;
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
}


