package com.salteddoubao.relicsystem.storage;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.RelicData;
import com.salteddoubao.relicsystem.relic.RelicSlot;
import com.salteddoubao.relicsystem.util.RelicItemConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于末影箱的圣遗物存储系统
 * 
 * 存储布局：
 * 末影箱共27格：
 * - 0-4:   已装备圣遗物 (FLOWER, PLUME, SANDS, GOBLET, CIRCLET)
 * - 5-26:  圣遗物仓库 (22格存储空间)
 */
public class EnderChestStorage {
    private final MinecraftRelicSystem plugin;
    private final RelicItemConverter converter;
    
    // 末影箱槽位分配
    private static final int EQUIPPED_START = 0;  // 已装备：0-4
    private static final int EQUIPPED_END = 4;
    private static final int WAREHOUSE_START = 5; // 仓库：5-26
    private static final int WAREHOUSE_END = 26;
    
    public EnderChestStorage(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        this.converter = plugin.getRelicItemConverter();
    }
    
    /**
     * 获取玩家已装备的圣遗物
     */
    public RelicData getEquipped(Player player, RelicSlot slot) {
        Inventory enderChest = player.getEnderChest();
        int slotIndex = EQUIPPED_START + slot.ordinal();
        
        ItemStack item = enderChest.getItem(slotIndex);
        if (converter.isRelicItem(item)) {
            return converter.fromItemStack(item);
        }
        return null;
    }
    
    /**
     * 装备圣遗物
     */
    public boolean equipRelic(Player player, RelicData relic) {
        if (relic == null) return false;
        
        Inventory enderChest = player.getEnderChest();
        int slotIndex = EQUIPPED_START + relic.getSlot().ordinal();
        
        // 如果原位置有装备，移动到仓库
        ItemStack oldEquipped = enderChest.getItem(slotIndex);
        if (converter.isRelicItem(oldEquipped)) {
            if (!addToWarehouse(player, converter.fromItemStack(oldEquipped))) {
                player.sendMessage("§c装备失败：仓库已满，无法放入原装备");
                return false;
            }
        }
        
        // 装备新圣遗物
        enderChest.setItem(slotIndex, converter.toItemStack(relic));
        
        // 从仓库中移除（如果存在）
        removeFromWarehouse(player, relic);
        
        return true;
    }
    
    /**
     * 卸下装备
     */
    public boolean unequipRelic(Player player, RelicSlot slot) {
        Inventory enderChest = player.getEnderChest();
        int slotIndex = EQUIPPED_START + slot.ordinal();
        
        ItemStack equipped = enderChest.getItem(slotIndex);
        if (!converter.isRelicItem(equipped)) {
            return false; // 该部位没有装备
        }
        
        RelicData relic = converter.fromItemStack(equipped);
        if (relic == null) return false;
        
        // 尝试放入仓库
        if (addToWarehouse(player, relic)) {
            enderChest.setItem(slotIndex, null);
            return true;
        } else {
            player.sendMessage("§c卸下失败：仓库已满");
            return false;
        }
    }
    
    /**
     * 获取仓库中的所有圣遗物
     */
    public List<RelicData> getWarehouse(Player player) {
        List<RelicData> warehouse = new ArrayList<>();
        Inventory enderChest = player.getEnderChest();
        
        for (int i = WAREHOUSE_START; i <= WAREHOUSE_END; i++) {
            ItemStack item = enderChest.getItem(i);
            if (converter.isRelicItem(item)) {
                RelicData relic = converter.fromItemStack(item);
                if (relic != null) {
                    warehouse.add(relic);
                }
            }
        }
        
        return warehouse;
    }
    
    /**
     * 获取指定部位的仓库圣遗物
     */
    public List<RelicData> getWarehouseBySlot(Player player, RelicSlot slot) {
        return getWarehouse(player).stream()
                .filter(r -> r.getSlot() == slot)
                .sorted((a, b) -> Integer.compare(b.getLevel(), a.getLevel()))
                .toList();
    }
    
    /**
     * 添加圣遗物到仓库
     */
    public boolean addToWarehouse(Player player, RelicData relic) {
        if (relic == null) return false;
        
        Inventory enderChest = player.getEnderChest();
        
        // 查找第一个空槽位
        for (int i = WAREHOUSE_START; i <= WAREHOUSE_END; i++) {
            ItemStack item = enderChest.getItem(i);
            if (item == null || item.getType().isAir()) {
                enderChest.setItem(i, converter.toItemStack(relic));
                return true;
            }
        }
        
        return false; // 仓库已满
    }
    
    /**
     * 从仓库移除圣遗物
     */
    public boolean removeFromWarehouse(Player player, RelicData relic) {
        if (relic == null) return false;
        
        Inventory enderChest = player.getEnderChest();
        
        for (int i = WAREHOUSE_START; i <= WAREHOUSE_END; i++) {
            ItemStack item = enderChest.getItem(i);
            if (converter.isRelicItem(item)) {
                RelicData existing = converter.fromItemStack(item);
                if (existing != null && existing.getId().equals(relic.getId())) {
                    enderChest.setItem(i, null);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 获取仓库可用空间
     */
    public int getWarehouseAvailableSpace(Player player) {
        Inventory enderChest = player.getEnderChest();
        int available = 0;
        
        for (int i = WAREHOUSE_START; i <= WAREHOUSE_END; i++) {
            ItemStack item = enderChest.getItem(i);
            if (item == null || item.getType().isAir()) {
                available++;
            }
        }
        
        return available;
    }
    
    /**
     * 获取仓库总容量
     */
    public int getWarehouseCapacity() {
        return WAREHOUSE_END - WAREHOUSE_START + 1; // 22格
    }
    
    /**
     * 清理末影箱中的无效圣遗物数据
     */
    public void cleanupInvalidRelics(Player player) {
        Inventory enderChest = player.getEnderChest();
        
        for (int i = 0; i < enderChest.getSize(); i++) {
            ItemStack item = enderChest.getItem(i);
            if (item != null && converter.isRelicItem(item)) {
                RelicData relic = converter.fromItemStack(item);
                if (relic == null) {
                    // 无效的圣遗物数据，清理掉
                    enderChest.setItem(i, null);
                    plugin.getLogger().warning("清理了玩家 " + player.getName() + " 末影箱中的无效圣遗物数据 (槽位" + i + ")");
                }
            }
        }
    }
}
