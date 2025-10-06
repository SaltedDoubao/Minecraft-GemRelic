package com.lymc.gemrelic.storage;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.relic.PlayerRelicProfile;
import com.lymc.gemrelic.relic.RelicData;
import com.lymc.gemrelic.relic.RelicSlot;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 基于独立存储的玩家圣遗物档案管理器
 * 
 * 使用类似末影箱的原理，但创建独立的存储空间：
 * - 不占用玩家末影箱
 * - 利用Minecraft原生ItemStack序列化机制
 * - 数据自动持久化，可靠性高
 */
public class InventoryProfileManager implements IRelicProfileManager {
    private final GemRelicPlugin plugin;
    private final RelicInventoryStorage storage;
    
    public InventoryProfileManager(GemRelicPlugin plugin) {
        this.plugin = plugin;
        this.storage = new RelicInventoryStorage(plugin);
    }
    
    /**
     * 获取玩家圣遗物档案
     * 直接从独立存储读取，无需内存缓存
     */
    @Override
    public PlayerRelicProfile get(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerRelicProfile profile = new InventoryPlayerProfile(playerId, player, storage);
        
        // 清理无效数据
        storage.cleanupInvalidRelics(player);
        
        return profile;
    }
    
    /**
     * 清理玩家档案（独立存储无需清理，数据自动持久化）
     */
    @Override
    public void clear(Player player) {
        // 独立存储不需要清理操作，数据已经实时持久化
        plugin.getLogger().info("玩家 " + player.getName() + " 的圣遗物数据已自动保存到独立存储");
    }
    
    /**
     * 保存玩家档案（独立存储无需手动保存）
     */
    @Override
    public void save(Player player) {
        // 独立存储无需手动保存，数据实时同步
        // feedback: 后台不再显示玩家圣遗物数据状态
        // 保留方法但不输出日志
    }
    
    /**
     * 保存所有在线玩家数据（独立存储无需此操作）
     */
    @Override
    public void saveAll() {
        // 保留提示但简化
        plugin.getLogger().fine("独立存储系统：所有玩家数据已自动持久化");
    }
    
    /**
     * 基于独立存储的玩家档案实现
     */
    private static class InventoryPlayerProfile extends PlayerRelicProfile {
        private final Player player;
        private final RelicInventoryStorage storage;
        
        public InventoryPlayerProfile(UUID playerId, Player player, RelicInventoryStorage storage) {
            super(playerId);
            this.player = player;
            this.storage = storage;
        }
        
        @Override
        public Map<RelicSlot, RelicData> getEquipped() {
            Map<RelicSlot, RelicData> equipped = new EnumMap<>(RelicSlot.class);
            for (RelicSlot slot : RelicSlot.values()) {
                RelicData relic = storage.getEquipped(player, slot);
                if (relic != null) {
                    equipped.put(slot, relic);
                }
            }
            return equipped;
        }
        
        @Override
        public List<RelicData> getWarehouse() {
            return storage.getWarehouse(player);
        }
        
        @Override
        public List<RelicData> getWarehouseBySlot(RelicSlot slot) {
            return storage.getWarehouseBySlot(player, slot);
        }
        
        @Override
        public void equip(RelicData relic) {
            if (relic == null) return;
            boolean success = storage.equipRelic(player, relic);
            if (!success) {
                player.sendMessage("§c装备失败");
            }
        }
        
        @Override
        public void unequip(RelicSlot slot) {
            boolean success = storage.unequipRelic(player, slot);
            if (success) {
                player.sendMessage("§a已卸下 " + getSlotDisplayName(slot) + " 部位的装备");
            } else {
                player.sendMessage("§c卸下失败");
            }
        }
        
        @Override
        public void addToWarehouse(RelicData relic) {
            if (relic == null) return;
            boolean success = storage.addToWarehouse(player, relic);
            if (!success) {
                player.sendMessage("§c仓库已满，无法添加圣遗物");
            }
        }
        
        @Override
        public boolean removeFromWarehouse(RelicData relic) {
            return storage.removeFromWarehouse(player, relic);
        }
        
        /**
         * 获取存储统计信息
         */
        public String getStorageStats() {
            return storage.getStorageStats(player);
        }
        
        /**
         * 获取仓库状态信息
         */
        public String getWarehouseStatus() {
            int used = getWarehouse().size();
            int capacity = storage.getWarehouseCapacity();
            int available = storage.getWarehouseAvailableSpace(player);
            
            return String.format("仓库状态: %d/%d (可用: %d)", used, capacity, available);
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
    
    /**
     * 获取存储实例（用于数据迁移等）
     */
    public RelicInventoryStorage getStorage() {
        return storage;
    }
}
