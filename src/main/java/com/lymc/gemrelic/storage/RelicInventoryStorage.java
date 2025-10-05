package com.lymc.gemrelic.storage;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.relic.RelicData;
import com.lymc.gemrelic.relic.RelicSlot;
import com.lymc.gemrelic.util.RelicItemConverter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于独立Inventory的圣遗物存储系统
 * 
 * 使用类似末影箱的原理，但创建独立的存储空间：
 * - 不占用玩家末影箱
 * - 利用Minecraft原生ItemStack序列化机制
 * - 数据自动持久化，可靠性高
 * 
 * 存储布局：
 * - 0-4:   已装备圣遗物 (FLOWER, PLUME, SANDS, GOBLET, CIRCLET)
 * - 5-26:  圣遗物仓库 (22格存储空间)
 */
public class RelicInventoryStorage {
    private final GemRelicPlugin plugin;
    private final RelicItemConverter converter;
    
    // 虚拟圣遗物仓库大小（类似末影箱的27格）
    private static final int INVENTORY_SIZE = 27;
    
    // 槽位分配
    private static final int EQUIPPED_START = 0;  // 已装备：0-4
    private static final int WAREHOUSE_START = 5; // 仓库：5-26
    private static final int WAREHOUSE_END = 26;
    
    public RelicInventoryStorage(GemRelicPlugin plugin) {
        this.plugin = plugin;
        RelicItemConverter providedConverter = plugin.getRelicItemConverter();
        if (providedConverter == null) {
            throw new IllegalStateException("RelicItemConverter 未初始化，请确保在插件初始化流程中首先创建");
        }
        this.converter = providedConverter;
    }
    
    /**
     * 获取玩家的圣遗物存储Inventory
     */
    public Inventory getRelicInventory(Player player) {
        // 创建虚拟的圣遗物存储空间
        Inventory relicInv = Bukkit.createInventory(null, INVENTORY_SIZE, "§6" + player.getName() + " 的圣遗物存储");
        
        // 从文件加载数据
        loadInventoryFromFile(player, relicInv);
        
        return relicInv;
    }
    
    /**
     * 保存圣遗物存储到文件
     */
    public void saveRelicInventory(Player player, Inventory inventory) {
        try {
            File dataFile = getStorageFile(player.getUniqueId());
            
            // 将Inventory序列化为字节数组（类似末影箱存储机制）
            byte[] inventoryData = serializeInventory(inventory);
            
            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(dataFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                bos.write(inventoryData);
                bos.flush();
            }
            
            plugin.getLogger().fine("已保存玩家 " + player.getName() + " 的圣遗物存储数据");
            
        } catch (Exception e) {
            plugin.getLogger().severe("保存玩家 " + player.getName() + " 圣遗物存储失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从文件加载圣遗物存储
     */
    private void loadInventoryFromFile(Player player, Inventory inventory) {
        try {
            File dataFile = getStorageFile(player.getUniqueId());
            
            if (!dataFile.exists()) {
                // 首次使用，初始化空存储
                return;
            }
            
            // 读取并反序列化数据
            byte[] inventoryData;
            try (FileInputStream fis = new FileInputStream(dataFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                inventoryData = bis.readAllBytes();
            }
            
            // 反序列化到Inventory
            deserializeInventory(inventoryData, inventory);
            
            plugin.getLogger().fine("已加载玩家 " + player.getName() + " 的圣遗物存储数据");
            
        } catch (Exception e) {
            plugin.getLogger().warning("加载玩家 " + player.getName() + " 圣遗物存储失败: " + e.getMessage());
            // 加载失败时保持空的存储，避免数据丢失
        }
    }
    
    /**
     * 获取存储文件路径
     */
    private File getStorageFile(UUID playerId) {
        File storageDir = new File(plugin.getDataFolder(), "relic_storage");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return new File(storageDir, playerId.toString() + ".dat");
    }
    
    /**
     * 序列化Inventory为字节数组（模拟Minecraft的存储机制）
     */
    private byte[] serializeInventory(Inventory inventory) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos);
        
        // 新版本：2（使用 Bukkit 对象流直接写入 ItemStack）
        oos.writeInt(2);
        
        // 写入存储大小
        int size = inventory.getSize();
        oos.writeInt(size);
        
        // 为每个槽位写入存在标志 + 物品
        for (int i = 0; i < size; i++) {
            ItemStack item = inventory.getItem(i);
            boolean present = (item != null && !item.getType().isAir());
            oos.writeBoolean(present);
            if (present) {
                oos.writeObject(item);
            }
        }
        
        oos.flush();
        oos.close();
        return baos.toByteArray();
    }
    
    /**
     * 从字节数组反序列化到Inventory
     */
    private void deserializeInventory(byte[] data, Inventory inventory) throws IOException, ClassNotFoundException {
        try (BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            int version = ois.readInt();
            if (version == 2) {
                int size = ois.readInt();
                if (size != inventory.getSize()) {
                    plugin.getLogger().warning("圣遗物存储大小不匹配: " + size + " vs " + inventory.getSize());
                }
                int limit = Math.min(size, inventory.getSize());
                for (int i = 0; i < limit; i++) {
                    boolean present = ois.readBoolean();
                    if (present) {
                        ItemStack item = (ItemStack) ois.readObject();
                        inventory.setItem(i, item);
                    }
                }
                return;
            }

            // 兼容旧版(v1)：槽位索引 + Map 序列化
            if (version == 1) {
                int size = ois.readInt();
                if (size != inventory.getSize()) {
                    plugin.getLogger().warning("圣遗物存储大小不匹配: " + size + " vs " + inventory.getSize());
                }
                while (true) {
                    int slot = ois.readInt();
                    if (slot == -1) break;
                    if (slot >= 0 && slot < inventory.getSize()) {
                        @SuppressWarnings("unchecked")
                        var itemData = (java.util.Map<String, Object>) ois.readObject();
                        ItemStack item = ItemStack.deserialize(itemData);
                        inventory.setItem(slot, item);
                    }
                }
                return;
            }

            plugin.getLogger().warning("未知的存储数据版本: " + version + ", 将忽略加载");
        }
    }
    
    /**
     * 获取已装备的圣遗物
     */
    public RelicData getEquipped(Player player, RelicSlot slot) {
        Inventory relicInv = getRelicInventory(player);
        int slotIndex = EQUIPPED_START + slot.ordinal();
        
        ItemStack item = relicInv.getItem(slotIndex);
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
        
        Inventory relicInv = getRelicInventory(player);
        int slotIndex = EQUIPPED_START + relic.getSlot().ordinal();
        
        // 如果原位置有装备，移动到仓库
        ItemStack oldEquipped = relicInv.getItem(slotIndex);
        if (converter.isRelicItem(oldEquipped)) {
            RelicData oldRelic = converter.fromItemStack(oldEquipped);
            if (oldRelic != null && !addToWarehouse(player, oldRelic, relicInv)) {
                player.sendMessage("§c装备失败：仓库已满，无法放入原装备");
                return false;
            }
        }
        
        // 装备新圣遗物
        relicInv.setItem(slotIndex, converter.toItemStack(relic));
        
        // 从仓库中移除（如果存在）
        removeFromWarehouse(player, relic, relicInv);
        
        // 保存数据
        saveRelicInventory(player, relicInv);
        
        return true;
    }
    
    /**
     * 卸下装备
     */
    public boolean unequipRelic(Player player, RelicSlot slot) {
        Inventory relicInv = getRelicInventory(player);
        int slotIndex = EQUIPPED_START + slot.ordinal();
        
        ItemStack equipped = relicInv.getItem(slotIndex);
        if (!converter.isRelicItem(equipped)) {
            return false; // 该部位没有装备
        }
        
        RelicData relic = converter.fromItemStack(equipped);
        if (relic == null) return false;
        
        // 尝试放入仓库
        if (addToWarehouse(player, relic, relicInv)) {
            relicInv.setItem(slotIndex, null);
            saveRelicInventory(player, relicInv);
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
        Inventory relicInv = getRelicInventory(player);
        
        for (int i = WAREHOUSE_START; i <= WAREHOUSE_END; i++) {
            ItemStack item = relicInv.getItem(i);
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
        Inventory relicInv = getRelicInventory(player);
        boolean success = addToWarehouse(player, relic, relicInv);
        if (success) {
            saveRelicInventory(player, relicInv);
        }
        return success;
    }
    
    private boolean addToWarehouse(Player player, RelicData relic, Inventory relicInv) {
        if (relic == null) return false;
        
        // 查找第一个空槽位
        for (int i = WAREHOUSE_START; i <= WAREHOUSE_END; i++) {
            ItemStack item = relicInv.getItem(i);
            if (item == null || item.getType().isAir()) {
                relicInv.setItem(i, converter.toItemStack(relic));
                return true;
            }
        }
        
        return false; // 仓库已满
    }
    
    /**
     * 从仓库移除圣遗物
     */
    public boolean removeFromWarehouse(Player player, RelicData relic) {
        Inventory relicInv = getRelicInventory(player);
        boolean success = removeFromWarehouse(player, relic, relicInv);
        if (success) {
            saveRelicInventory(player, relicInv);
        }
        return success;
    }
    
    private boolean removeFromWarehouse(Player player, RelicData relic, Inventory relicInv) {
        if (relic == null) return false;
        
        for (int i = WAREHOUSE_START; i <= WAREHOUSE_END; i++) {
            ItemStack item = relicInv.getItem(i);
            if (converter.isRelicItem(item)) {
                RelicData existing = converter.fromItemStack(item);
                if (existing != null && existing.getId().equals(relic.getId())) {
                    relicInv.setItem(i, null);
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
        Inventory relicInv = getRelicInventory(player);
        int available = 0;
        
        for (int i = WAREHOUSE_START; i <= WAREHOUSE_END; i++) {
            ItemStack item = relicInv.getItem(i);
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
     * 清理无效的圣遗物数据
     */
    public void cleanupInvalidRelics(Player player) {
        Inventory relicInv = getRelicInventory(player);
        boolean hasChanges = false;
        
        for (int i = 0; i < relicInv.getSize(); i++) {
            ItemStack item = relicInv.getItem(i);
            if (item != null && converter.isRelicItem(item)) {
                RelicData relic = converter.fromItemStack(item);
                if (relic == null) {
                    // 无效的圣遗物数据，清理掉
                    relicInv.setItem(i, null);
                    hasChanges = true;
                    plugin.getLogger().warning("清理了玩家 " + player.getName() + " 圣遗物存储中的无效数据 (槽位" + i + ")");
                }
            }
        }
        
        if (hasChanges) {
            saveRelicInventory(player, relicInv);
        }
    }
    
    /**
     * 获取存储统计信息
     */
    public String getStorageStats(Player player) {
        int equipped = 0;
        for (RelicSlot slot : RelicSlot.values()) {
            if (getEquipped(player, slot) != null) {
                equipped++;
            }
        }
        
        int warehouseUsed = getWarehouse(player).size();
        int warehouseCapacity = getWarehouseCapacity();
        int warehouseAvailable = getWarehouseAvailableSpace(player);
        
        return String.format("装备: %d/5, 仓库: %d/%d (可用: %d)", 
                equipped, warehouseUsed, warehouseCapacity, warehouseAvailable);
    }
}
