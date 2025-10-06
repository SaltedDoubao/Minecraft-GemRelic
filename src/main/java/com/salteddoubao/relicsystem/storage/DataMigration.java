package com.salteddoubao.relicsystem.storage;

import org.bukkit.entity.Player;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.manager.RelicProfileManager;
import com.salteddoubao.relicsystem.relic.PlayerRelicProfile;
import com.salteddoubao.relicsystem.relic.RelicData;
import com.salteddoubao.relicsystem.relic.RelicSlot;

import java.io.File;
import java.util.Map;

/**
 * 数据迁移工具：从YAML存储迁移到独立存储系统
 */
public class DataMigration {
    private final MinecraftRelicSystem plugin;
    private final RelicProfileManager oldManager;
    private final InventoryProfileManager newManager;
    
    public DataMigration(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        this.oldManager = new RelicProfileManager(plugin);
        this.newManager = new InventoryProfileManager(plugin);
    }
    
    /**
     * 迁移单个玩家的数据
     */
    public boolean migratePlayer(Player player) {
        try {
            plugin.getLogger().info("开始迁移玩家数据: " + player.getName());
            
            // 检查是否有旧数据文件
            File oldDataFile = new File(plugin.getDataFolder(), "players/" + player.getUniqueId().toString() + ".yml");
            if (!oldDataFile.exists()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 没有旧数据文件，跳过迁移");
                return true;
            }
            
            // 加载旧数据
            PlayerRelicProfile oldProfile = oldManager.get(player);
            
            // 检查独立存储是否有足够空间
            RelicInventoryStorage storage = newManager.getStorage();
            int requiredSpace = oldProfile.getWarehouse().size();
            int availableSpace = storage.getWarehouseAvailableSpace(player);
            
            if (requiredSpace > availableSpace) {
                player.sendMessage("§c数据迁移失败：独立存储空间不足");
                player.sendMessage("§c需要 " + requiredSpace + " 格空间，但只有 " + availableSpace + " 格可用");
                plugin.getLogger().warning("玩家 " + player.getName() + " 独立存储空间不足，迁移失败");
                return false;
            }
            
            // 获取新的档案管理器
            PlayerRelicProfile newProfile = newManager.get(player);
            
            int migratedEquipped = 0;
            int migratedWarehouse = 0;
            
            // 迁移已装备的圣遗物
            for (Map.Entry<RelicSlot, RelicData> entry : oldProfile.getEquipped().entrySet()) {
                RelicData relic = entry.getValue();
                if (relic != null) {
                    newProfile.equip(relic);
                    migratedEquipped++;
                }
            }
            
            // 迁移仓库中的圣遗物
            for (RelicData relic : oldProfile.getWarehouse()) {
                newProfile.addToWarehouse(relic);
                migratedWarehouse++;
            }
            
            plugin.getLogger().info("玩家 " + player.getName() + " 数据迁移完成: " + 
                "装备 " + migratedEquipped + " 件, 仓库 " + migratedWarehouse + " 件");
            
            player.sendMessage("§a数据迁移完成！");
            player.sendMessage("§a已迁移：装备 " + migratedEquipped + " 件，仓库 " + migratedWarehouse + " 件");
            player.sendMessage("§e现在您的圣遗物数据存储在独立存储系统中，更加安全可靠！");
            player.sendMessage("§7提示：采用类似末影箱的机制，但不占用您的末影箱空间");
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("迁移玩家 " + player.getName() + " 数据时发生错误: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§c数据迁移过程中发生错误，请联系管理员");
            return false;
        }
    }
    
    /**
     * 备份旧数据文件
     */
    public boolean backupOldData(Player player) {
        try {
            File oldDataFile = new File(plugin.getDataFolder(), "players/" + player.getUniqueId().toString() + ".yml");
            if (!oldDataFile.exists()) {
                return true; // 没有旧文件，无需备份
            }
            
            File backupDir = new File(plugin.getDataFolder(), "migration_backup");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            File backupFile = new File(backupDir, player.getUniqueId().toString() + "_" + System.currentTimeMillis() + ".yml");
            
            if (oldDataFile.renameTo(backupFile)) {
                plugin.getLogger().info("已备份玩家 " + player.getName() + " 的旧数据到: " + backupFile.getName());
                return true;
            } else {
                plugin.getLogger().warning("无法备份玩家 " + player.getName() + " 的旧数据");
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("备份玩家 " + player.getName() + " 数据时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 检查玩家是否需要迁移
     */
    public boolean needsMigration(Player player) {
        File oldDataFile = new File(plugin.getDataFolder(), "players/" + player.getUniqueId().toString() + ".yml");
        return oldDataFile.exists();
    }
    
    /**
     * 获取迁移统计信息
     */
    public String getMigrationStats() {
        File playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists() || !playersDir.isDirectory()) {
            return "没有发现需要迁移的数据文件";
        }
        
        File[] files = playersDir.listFiles((dir, name) -> name.endsWith(".yml"));
        int oldDataCount = files != null ? files.length : 0;
        
        File backupDir = new File(plugin.getDataFolder(), "migration_backup");
        File[] backupFiles = backupDir.exists() ? backupDir.listFiles() : null;
        int backupCount = backupFiles != null ? backupFiles.length : 0;
        
        return String.format("迁移统计: 待迁移 %d 个文件, 已备份 %d 个文件", oldDataCount, backupCount);
    }
}
