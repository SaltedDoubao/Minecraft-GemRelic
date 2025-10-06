package com.salteddoubao.relicsystem.storage;

import org.bukkit.entity.Player;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.PlayerRelicProfile;
import com.salteddoubao.relicsystem.relic.RelicData;
import com.salteddoubao.relicsystem.relic.RelicSlot;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 基于末影箱的玩家圣遗物档案管理器
 * 替代传统的YAML文件存储，使用Minecraft原生末影箱系统
 */
public class EnderChestProfileManager implements IRelicProfileManager {
    private final MinecraftRelicSystem plugin;
    private final EnderChestStorage storage;
    
    public EnderChestProfileManager(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        this.storage = new EnderChestStorage(plugin);
    }
    
    /**
     * 获取玩家圣遗物档案
     * 直接从末影箱读取，无需内存缓存
     */
    public PlayerRelicProfile get(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerRelicProfile profile = new EnderChestPlayerProfile(playerId, player, storage);
        
        // 清理无效数据
        storage.cleanupInvalidRelics(player);
        
        return profile;
    }
    
    /**
     * 清理玩家档案（末影箱存储无需清理，数据自动持久化）
     */
    public void clear(Player player) {
        // 末影箱存储不需要清理操作，数据已经实时持久化
        plugin.getLogger().info("玩家 " + player.getName() + " 的圣遗物数据已自动保存到末影箱");
    }
    
    /**
     * 保存玩家档案（末影箱存储无需手动保存）
     */
    public void save(Player player) {
        // 末影箱存储无需手动保存，数据实时同步
        // 只记录日志用于调试
        PlayerRelicProfile profile = get(player);
        int equippedCount = (int) profile.getEquipped().values().stream().filter(r -> r != null).count();
        int warehouseCount = profile.getWarehouse().size();
        
        plugin.getLogger().info("玩家圣遗物数据状态: " + player.getName() + 
            " (装备:" + equippedCount + "件, 仓库:" + warehouseCount + "件) - 末影箱自动持久化");
    }
    
    /**
     * 保存所有在线玩家数据（末影箱存储无需此操作）
     */
    public void saveAll() {
        plugin.getLogger().info("末影箱存储系统：所有玩家数据已自动持久化，无需手动保存");
    }
    
    /**
     * 基于末影箱的玩家档案实现
     */
    private static class EnderChestPlayerProfile extends PlayerRelicProfile {
        private final Player player;
        private final EnderChestStorage storage;
        
        public EnderChestPlayerProfile(UUID playerId, Player player, EnderChestStorage storage) {
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
            if (success) {
                player.sendMessage("§a已装备圣遗物到 " + getSlotDisplayName(relic.getSlot()) + " 部位");
            } else {
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
}
