package com.salteddoubao.relicsystem.listener;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.gui.*;
import com.salteddoubao.relicsystem.relic.*;
import com.salteddoubao.relicsystem.util.RelicItemConverter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelicGUIListener implements Listener {
    private final MinecraftRelicSystem plugin;
    private static final Pattern WAREHOUSE_TITLE = Pattern.compile("§b§l圣遗物仓库.*\\((\\d+)\\)");
    
    public RelicGUIListener(MinecraftRelicSystem plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        boolean topIsRelicGui = e.getView().getTopInventory() != null && e.getView().getTopInventory().getHolder() instanceof RelicMainMenuGUI.Holder
                || e.getView().getTopInventory() != null && e.getView().getTopInventory().getHolder() instanceof RelicEquipmentGUI.Holder
                || e.getView().getTopInventory() != null && e.getView().getTopInventory().getHolder() instanceof RelicWarehouseGUI.Holder;
        
        // 主菜单
        if (title.equals(RelicMainMenuGUI.TITLE) && topIsRelicGui) {
            e.setCancelled(true);
            // 只处理顶部GUI的点击，不处理玩家背包的点击
            if (e.getClickedInventory() == null || e.getClickedInventory().getHolder() == null) {
                return;
            }
            handleMainMenuClick(player, e);
            return;
        }
        
        // 装备页面
        if (title.equals(RelicEquipmentGUI.TITLE) && topIsRelicGui) {
            e.setCancelled(true);
            // 只处理顶部GUI的点击
            if (e.getClickedInventory() == null || e.getClickedInventory().getHolder() == null) {
                return;
            }
            handleEquipmentClick(player, e);
            return;
        }
        
        // 仓库界面
        if (title.startsWith(RelicWarehouseGUI.TITLE_PREFIX) && topIsRelicGui) {
            if (e.getClickedInventory() == null) return;

            // 顶部GUI点击：完全接管
            if (e.getClickedInventory().getHolder() instanceof RelicWarehouseGUI.Holder) {
                e.setCancelled(true);

                // 若鼠标携带圣遗物并点击仓库区域，则放入仓库
                if (isWarehouseSlot(e.getSlot())) {
                    ItemStack cursor = e.getCursor();
                    if (plugin.getRelicItemConverter().isRelicItem(cursor)) {
                        RelicData relic = plugin.getRelicItemConverter().fromItemStack(cursor);
                        if (relic != null) {
                            PlayerRelicProfile profile = plugin.getRelicProfileManager().get(player);
                            profile.addToWarehouse(relic);
                            player.setItemOnCursor(null);
                            plugin.getRelicProfileManager().save(player);
                            // 刷新当前页面
                            handleRefreshWarehouse(player, title);
                            return;
                        }
                    }
                }

                handleWarehouseClick(player, e, title);
                return;
            }

            // 底部背包点击：支持Shift-点击快速存入仓库
            if (!(e.getClickedInventory().getHolder() instanceof RelicWarehouseGUI.Holder)) {
                ItemStack clicked = e.getCurrentItem();
                if (clicked != null && plugin.getRelicItemConverter().isRelicItem(clicked)
                        && (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT)) {
                    e.setCancelled(true);
                    RelicData relic = plugin.getRelicItemConverter().fromItemStack(clicked);
                    if (relic != null) {
                        PlayerRelicProfile profile = plugin.getRelicProfileManager().get(player);
                        profile.addToWarehouse(relic);
                        // 清空该格物品
                        e.getClickedInventory().setItem(e.getSlot(), null);
                        plugin.getRelicProfileManager().save(player);
                        // 刷新当前页面
                        handleRefreshWarehouse(player, title);
                    }
                }
                return;
            }
        }
    }
    
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        
        // 阻止在圣遗物GUI中的拖动操作
        if (title.equals(RelicMainMenuGUI.TITLE) || 
            title.equals(RelicEquipmentGUI.TITLE) || 
            title.startsWith(RelicWarehouseGUI.TITLE_PREFIX)) {
            e.setCancelled(true);
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
                plugin.getRelicProfileManager().save(player);
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
            plugin.getRelicProfileManager().save(player);
            return;
        }
        
        // 点击仓库物品
        if (isWarehouseSlot(slot)) {
            RelicWarehouseGUI.SortMode sortMode = parseCurrentSortMode(title);
            List<RelicData> items = getSortedWarehouse(profile, currentFilter, sortMode);
            
            int itemIndex = getWarehouseIndex(slot) + currentPage * RelicWarehouseGUI.getWarehouseSlots().length;
            if (itemIndex < items.size()) {
                RelicData relic = items.get(itemIndex);
                
                if (clickType == ClickType.LEFT) {
                    // 左键：装备
                    profile.equip(relic);
                    plugin.getRelicEffectService().refresh(player, profile);
                    plugin.getRelicProfileManager().save(player);
                    // player.sendMessage("§a已装备圣遗物到 " + getSlotDisplayName(relic.getSlot()) + ": " + plugin.getRelicManager().getRelicSet(relic.getSetId()).getName());
                } else if (clickType == ClickType.RIGHT) {
                    // 右键：取出到背包
                    ItemStack relicItem = converter.toItemStack(relic);
                    if (player.getInventory().firstEmpty() != -1) {
                        profile.removeFromWarehouse(relic);
                        player.getInventory().addItem(relicItem);
                        plugin.getRelicProfileManager().save(player);
                        player.sendMessage("§a已取出圣遗物到背包");
                    } else {
                        player.sendMessage("§c背包已满，无法取出");
                    }
                }
                
                // 刷新仓库页面，保持排序
                new RelicWarehouseGUI(plugin).open(player, currentFilter, currentPage, sortMode);
            }
            return;
        }
        
        // 控制按钮
        if (slot == RelicWarehouseGUI.getPrevPageSlot() && currentPage > 0) {
            RelicWarehouseGUI.SortMode sortMode = parseCurrentSortMode(title);
            new RelicWarehouseGUI(plugin).open(player, currentFilter, currentPage - 1, sortMode);
        } else if (slot == RelicWarehouseGUI.getNextPageSlot()) {
            RelicWarehouseGUI.SortMode sortMode = parseCurrentSortMode(title);
            new RelicWarehouseGUI(plugin).open(player, currentFilter, currentPage + 1, sortMode);
        } else if (slot == RelicWarehouseGUI.getFilterSlot()) {
            // 循环切换筛选条件
            RelicWarehouseGUI.SortMode sortMode = parseCurrentSortMode(title);
            RelicSlot nextFilter = getNextFilter(currentFilter);
            new RelicWarehouseGUI(plugin).open(player, nextFilter, 0, sortMode);
        } else if (slot == RelicWarehouseGUI.getSortSlot()) {
            // 切换排序方式
            RelicWarehouseGUI.SortMode sortMode = parseCurrentSortMode(title);
            RelicWarehouseGUI.SortMode next = getNextSortMode(sortMode);
            new RelicWarehouseGUI(plugin).open(player, currentFilter, 0, next);
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
            plugin.getRelicProfileManager().save(player);
            // 刷新仓库页面
            // 保持当前排序
            // 注意：无法直接获知排序，只能在外层调用处传入。此处回退为默认排序。
            new RelicWarehouseGUI(plugin).open(player, currentFilter, currentPage);
        } else {
            player.sendMessage("§c背包中没有圣遗物物品");
        }
    }

    private void handleRefreshWarehouse(Player player, String title) {
        RelicSlot currentFilter = parseCurrentFilter(title);
        int currentPage = parseCurrentPage(title);
        RelicWarehouseGUI.SortMode sortMode = parseCurrentSortMode(title);
        new RelicWarehouseGUI(plugin).open(player, currentFilter, currentPage, sortMode);
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

    private RelicWarehouseGUI.SortMode parseCurrentSortMode(String title) {
        for (RelicWarehouseGUI.SortMode mode : RelicWarehouseGUI.SortMode.values()) {
            if (title.contains(mode.getDisplay())) {
                return mode;
            }
        }
        return RelicWarehouseGUI.SortMode.RARITY_DESC;
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

    private RelicWarehouseGUI.SortMode getNextSortMode(RelicWarehouseGUI.SortMode current) {
        RelicWarehouseGUI.SortMode[] modes = RelicWarehouseGUI.SortMode.values();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == current) {
                return i == modes.length - 1 ? modes[0] : modes[i + 1];
            }
        }
        return RelicWarehouseGUI.SortMode.RARITY_DESC;
    }

    private List<RelicData> getSortedWarehouse(PlayerRelicProfile profile, RelicSlot filter, RelicWarehouseGUI.SortMode mode) {
        List<RelicData> list = new ArrayList<>(filter != null ? profile.getWarehouseBySlot(filter) : profile.getWarehouse());
        Comparator<RelicData> byRarityDesc = Comparator.comparingInt((RelicData r) -> r.getRarity().getStars()).reversed();
        Comparator<RelicData> byLevelDesc = Comparator.comparingInt(RelicData::getLevel).reversed();
        Comparator<RelicData> bySetAsc = Comparator.comparing((RelicData r) -> r.getSetId(), String.CASE_INSENSITIVE_ORDER);
        Comparator<RelicData> bySlotAsc = Comparator.comparingInt(r -> r.getSlot().ordinal());
        Comparator<RelicData> byLockedFirst = Comparator.comparing((RelicData r) -> !r.isLocked());

        Comparator<RelicData> tiebreakers = byRarityDesc.thenComparing(byLevelDesc).thenComparing(bySetAsc).thenComparing(bySlotAsc);
        Comparator<RelicData> cmp;
        switch (mode) {
            case RARITY_DESC -> cmp = byRarityDesc.thenComparing(byLevelDesc).thenComparing(bySetAsc).thenComparing(bySlotAsc);
            case LEVEL_DESC -> cmp = byLevelDesc.thenComparing(byRarityDesc).thenComparing(bySetAsc).thenComparing(bySlotAsc);
            case SET_ASC -> cmp = bySetAsc.thenComparing(byRarityDesc).thenComparing(byLevelDesc).thenComparing(bySlotAsc);
            case SLOT_ASC -> cmp = bySlotAsc.thenComparing(byRarityDesc).thenComparing(byLevelDesc).thenComparing(bySetAsc);
            case LOCKED_FIRST -> cmp = byLockedFirst.thenComparing(tiebreakers);
            default -> cmp = tiebreakers;
        }
        list.sort(cmp);
        return list;
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


