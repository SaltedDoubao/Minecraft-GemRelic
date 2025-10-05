package com.lymc.gemrelic.storage;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.manager.RelicProfileManager;
import org.bukkit.entity.Player;

/**
 * 存储工厂类 - 统一管理不同的存储方式
 */
public class StorageFactory {
    private final GemRelicPlugin plugin;
    
    public enum StorageMode {
        YAML,      // 传统YAML文件存储
        INVENTORY  // 独立的圣遗物存储系统（类似末影箱原理）
    }
    
    public StorageFactory(GemRelicPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 获取配置的存储模式
     */
    public StorageMode getStorageMode() {
        String mode = plugin.getConfig().getString("relic.storage_mode", "INVENTORY");
        try {
            return StorageMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的存储模式: " + mode + "，使用默认的 INVENTORY 模式");
            return StorageMode.INVENTORY;
        }
    }
    
    /**
     * 创建相应的档案管理器
     */
    public IRelicProfileManager createProfileManager() {
        StorageMode mode = getStorageMode();
        
        switch (mode) {
            case YAML:
                plugin.getLogger().info("使用 YAML 文件存储模式");
                return new RelicProfileManager(plugin);
                
            case INVENTORY:
                plugin.getLogger().info("使用独立存储模式（推荐）- 类似末影箱原理，但不占用末影箱");
                return new InventoryProfileManager(plugin);
                
            default:
                plugin.getLogger().warning("未知存储模式，回退到 YAML 模式");
                return new RelicProfileManager(plugin);
        }
    }
    
    /**
     * 检查是否启用自动迁移
     */
    public boolean isAutoMigrationEnabled() {
        return plugin.getConfig().getBoolean("relic.inventory_storage.auto_migrate", true);
    }
    
    /**
     * 检查是否保留备份
     */
    public boolean shouldKeepBackup() {
        return plugin.getConfig().getBoolean("relic.inventory_storage.keep_backup", true);
    }
    
    /**
     * 执行自动迁移（如果需要）
     */
    public void performAutoMigration(Player player) {
        StorageMode mode = getStorageMode();
        
        // 只有在独立存储模式且启用自动迁移时才执行
        if (mode != StorageMode.INVENTORY || !isAutoMigrationEnabled()) {
            return;
        }
        
        DataMigration migration = new DataMigration(plugin);
        
        // 检查是否需要迁移
        if (!migration.needsMigration(player)) {
            return;
        }
        
        plugin.getLogger().info("为玩家 " + player.getName() + " 执行自动数据迁移...");
        
        // 备份旧数据
        if (shouldKeepBackup()) {
            if (!migration.backupOldData(player)) {
                plugin.getLogger().warning("玩家 " + player.getName() + " 的数据备份失败，跳过自动迁移");
                player.sendMessage("§c自动数据迁移失败：备份错误");
                player.sendMessage("§e请使用 §6/relic migrate §e命令手动迁移");
                return;
            }
        }
        
        // 执行迁移
        boolean success = migration.migratePlayer(player);
        if (success) {
            player.sendMessage("§a§l数据迁移完成！");
            player.sendMessage("§e您的圣遗物现在存储在独立的存储系统中，更加安全可靠！");
            player.sendMessage("§7提示：数据采用类似末影箱的机制，但不占用您的末影箱空间");
        } else {
            plugin.getLogger().warning("玩家 " + player.getName() + " 的自动迁移失败");
            player.sendMessage("§c自动数据迁移失败");
            player.sendMessage("§e请使用 §6/relic migrate §e命令手动迁移");
        }
    }
    
    /**
     * 获取存储模式的显示名称
     */
    public String getStorageModeDisplayName() {
        StorageMode mode = getStorageMode();
        return switch (mode) {
            case YAML -> "YAML文件存储";
            case INVENTORY -> "独立存储系统";
        };
    }
    
    /**
     * 获取存储模式的描述
     */
    public String getStorageModeDescription() {
        StorageMode mode = getStorageMode();
        return switch (mode) {
            case YAML -> "传统的YAML文件存储方式，兼容旧版本";
            case INVENTORY -> "独立的圣遗物存储系统，类似末影箱原理但不占用末影箱，数据更可靠";
        };
    }
}
