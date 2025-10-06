package com.salteddoubao.relicplugin.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import com.salteddoubao.relicplugin.MinecraftRelicSystem;
import com.salteddoubao.relicplugin.relic.RelicData;
import com.salteddoubao.relicplugin.relic.RelicSlot;
import com.salteddoubao.relicplugin.util.RelicItemConverter;

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
    private final MinecraftRelicSystem plugin;
    private final RelicItemConverter converter;
    
    // 旧版兼容：虚拟圣遗物仓库大小（用于v1/v2反序列化到临时Inventory）
    private static final int INVENTORY_SIZE = 27;
    
    // 槽位分配（装备位固定5个）
    private static final int EQUIPPED_START = 0;  // 已装备：0-4
    private static final int WAREHOUSE_START = 5; // 仓库：5-26
    private static final int WAREHOUSE_END = 26;

    // 新版（v3）仓库最大容量
    private static final int MAX_WAREHOUSE_CAPACITY = 2000;
    
    public RelicInventoryStorage(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        RelicItemConverter providedConverter = plugin.getRelicItemConverter();
        if (providedConverter == null) {
            throw new IllegalStateException("RelicItemConverter 未初始化，请确保在插件初始化流程中首先创建");
        }
        this.converter = providedConverter;
    }
    
    // ===== 新版(v3) 存取逻辑：装备位 + 动态仓库（最大2000） =====
    private static class StorageData {
        ItemStack[] equipped = new ItemStack[RelicSlot.values().length]; // 仅使用前5个
        List<ItemStack> warehouse = new ArrayList<>();
    }

    private StorageData loadStorageData(Player player) {
        StorageData data = new StorageData();
        try {
            File file = getStorageFile(player.getUniqueId());
            if (!file.exists()) {
                return data; // 空数据
            }
            byte[] bytes;
            try (FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis)) {
                bytes = bis.readAllBytes();
            }

            // 读取版本号（使用Bukkit对象流以正确跳过序列化头）
            int version;
            try (BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                version = ois.readInt();
            }

            if (version == 3) {
                try (BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                    ois.readInt(); // consume version
                    // 读取装备位（按枚举顺序）
                    for (int i = 0; i < RelicSlot.values().length; i++) {
                        boolean present = ois.readBoolean();
                        if (present) {
                            data.equipped[i] = (ItemStack) ois.readObject();
                        }
                    }
                    // 读取仓库列表
                    int count = ois.readInt();
                    for (int i = 0; i < count; i++) {
                        ItemStack item = (ItemStack) ois.readObject();
                        data.warehouse.add(item);
                    }
                }
                return data;
            }

            // 兼容旧版：v1/v2 读取到临时Inventory后拆分
            Inventory temp = Bukkit.createInventory(null, INVENTORY_SIZE);
            deserializeInventory(bytes, temp);
            for (int i = 0; i < RelicSlot.values().length; i++) {
                data.equipped[i] = temp.getItem(EQUIPPED_START + i);
            }
            for (int i = WAREHOUSE_START; i <= WAREHOUSE_END; i++) {
                ItemStack it = temp.getItem(i);
                if (it != null && !it.getType().isAir()) {
                    data.warehouse.add(it);
                }
            }
            return data;
        } catch (Exception e) {
            plugin.getLogger().warning("读取圣遗物存储失败: " + e.getMessage());
            return data;
        }
    }

    private void saveStorageData(Player player, StorageData data) {
        try {
            File dataFile = getStorageFile(player.getUniqueId());
            try (FileOutputStream fos = new FileOutputStream(dataFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
                // 写入版本
                oos.writeInt(3);
                // 写入装备位
                for (int i = 0; i < RelicSlot.values().length; i++) {
                    ItemStack it = data.equipped[i];
                    boolean present = (it != null && !it.getType().isAir());
                    oos.writeBoolean(present);
                    if (present) {
                        oos.writeObject(it);
                    }
                }
                // 写入仓库
                oos.writeInt(data.warehouse.size());
                for (ItemStack it : data.warehouse) {
                    oos.writeObject(it);
                }
                oos.flush();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("保存圣遗物存储失败: " + e.getMessage());
        }
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
            if (version == 3) {
                // v3：装备位+列表，不在此方法解析（该方法保留给旧版兼容）
                plugin.getLogger().fine("检测到v3数据格式，改用新版解析路径");
                return;
            }
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
        StorageData data = loadStorageData(player);
        ItemStack item = data.equipped[slot.ordinal()];
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
        StorageData data = loadStorageData(player);
        int idx = relic.getSlot().ordinal();

        // 如果原位置有装备，尝试放入仓库
        ItemStack oldEquipped = data.equipped[idx];
        if (converter.isRelicItem(oldEquipped)) {
            if (data.warehouse.size() >= MAX_WAREHOUSE_CAPACITY) {
                player.sendMessage("§c装备失败：仓库已满，无法放入原装备");
                return false;
            }
            data.warehouse.add(oldEquipped);
        }

        // 装备新圣遗物
        data.equipped[idx] = converter.toItemStack(relic);

        // 从仓库中移除同ID（如果存在）
        data.warehouse.removeIf(it -> {
            if (!converter.isRelicItem(it)) return false;
            RelicData rd = converter.fromItemStack(it);
            return rd != null && rd.getId().equals(relic.getId());
        });

        saveStorageData(player, data);
        return true;
    }
    
    /**
     * 卸下装备
     */
    public boolean unequipRelic(Player player, RelicSlot slot) {
        StorageData data = loadStorageData(player);
        int idx = slot.ordinal();
        ItemStack equipped = data.equipped[idx];
        if (!converter.isRelicItem(equipped)) {
            return false;
        }
        if (data.warehouse.size() >= MAX_WAREHOUSE_CAPACITY) {
            player.sendMessage("§c卸下失败：仓库已满");
            return false;
        }
        data.warehouse.add(equipped);
        data.equipped[idx] = null;
        saveStorageData(player, data);
        return true;
    }
    
    /**
     * 获取仓库中的所有圣遗物
     */
    public List<RelicData> getWarehouse(Player player) {
        StorageData data = loadStorageData(player);
        List<RelicData> list = new ArrayList<>();
        int idx = 0;
        for (ItemStack it : data.warehouse) {
            if (it == null || it.getType().isAir()) { idx++; continue; }
            if (!converter.isRelicItem(it)) { idx++; continue; }
            RelicData rd = converter.fromItemStack(it);
            if (rd != null) {
                list.add(rd);
            } else {
                plugin.getLogger().fine("忽略无效仓库条目 index=" + idx + " player=" + player.getName());
            }
            idx++;
        }
        return list;
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
        StorageData data = loadStorageData(player);
        if (relic == null) return false;
        if (data.warehouse.size() >= MAX_WAREHOUSE_CAPACITY) return false;
        data.warehouse.add(converter.toItemStack(relic));
        saveStorageData(player, data);
        return true;
    }
    
    private boolean addToWarehouse(Player player, RelicData relic, Inventory relicInv) {
        // 仅用于旧逻辑兼容（不再使用）。
        return false;
    }
    
    /**
     * 从仓库移除圣遗物
     */
    public boolean removeFromWarehouse(Player player, RelicData relic) {
        StorageData data = loadStorageData(player);
        if (relic == null) return false;
        boolean removed = data.warehouse.removeIf(it -> {
            if (!converter.isRelicItem(it)) return false;
            RelicData rd = converter.fromItemStack(it);
            return rd != null && rd.getId().equals(relic.getId());
        });
        if (removed) saveStorageData(player, data);
        return removed;
    }
    
    private boolean removeFromWarehouse(Player player, RelicData relic, Inventory relicInv) {
        // 仅用于旧逻辑兼容（不再使用）。
        return false;
    }
    
    /**
     * 获取仓库可用空间
     */
    public int getWarehouseAvailableSpace(Player player) {
        StorageData data = loadStorageData(player);
        return Math.max(0, MAX_WAREHOUSE_CAPACITY - data.warehouse.size());
    }
    
    /**
     * 获取仓库总容量
     */
    public int getWarehouseCapacity() {
        return MAX_WAREHOUSE_CAPACITY;
    }
    
    /**
     * 清理无效的圣遗物数据
     */
    public void cleanupInvalidRelics(Player player) {
        StorageData data = loadStorageData(player);
        boolean changed = false;
        // 校验装备位
        for (int i = 0; i < RelicSlot.values().length; i++) {
            ItemStack it = data.equipped[i];
            if (it != null && converter.isRelicItem(it)) {
                RelicData rd = converter.fromItemStack(it);
                if (rd == null) {
                    data.equipped[i] = null;
                    changed = true;
                    plugin.getLogger().warning("清理了玩家 " + player.getName() + " 圣遗物装备中的无效数据 (槽位" + i + ")");
                }
            }
        }
        // 校验仓库
        int before = data.warehouse.size();
        data.warehouse.removeIf(it -> converter.isRelicItem(it) && converter.fromItemStack(it) == null);
        if (before != data.warehouse.size()) changed = true;
        if (changed) saveStorageData(player, data);
    }
    
    /**
     * 获取存储统计信息
     */
    public String getStorageStats(Player player) {
        StorageData data = loadStorageData(player);
        int equipped = 0;
        for (int i = 0; i < RelicSlot.values().length; i++) {
            ItemStack it = data.equipped[i];
            if (it != null && !it.getType().isAir()) equipped++;
        }
        int warehouseUsed = data.warehouse.size();
        int warehouseCapacity = MAX_WAREHOUSE_CAPACITY;
        int warehouseAvailable = Math.max(0, warehouseCapacity - warehouseUsed);
        return String.format("装备: %d/5, 仓库: %d/%d (可用: %d)", 
                equipped, warehouseUsed, warehouseCapacity, warehouseAvailable);
    }
}

