package com.salteddoubao.relicsystem.util;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 配置管理器
 * 缓存常用配置值，避免重复读取配置文件
 * 
 * @author SaltedDoubao
 * @version 1.0.0
 */
public class ConfigManager {
    private final MinecraftRelicSystem plugin;

    // 缓存常用配置
    private boolean debugMode;
    private boolean debugEffects;
    private int maxLevel;
    private boolean autoMigration;
    private String storageMode;
    private boolean attributePlusEnabled;
    private String attributePlusNamespace;

    public ConfigManager(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.debugMode = config.getBoolean("settings.debug", false);
        this.debugEffects = config.getBoolean("settings.relic.debug_effects", false);
        this.maxLevel = config.getInt("relic.max_level", 20);
        this.autoMigration = config.getBoolean("relic.inventory_storage.auto_migrate", true);
        this.storageMode = config.getString("relic.storage_mode", "YAML");
        this.attributePlusEnabled = config.getBoolean("integration.attributeplus.enabled", false);
        this.attributePlusNamespace = config.getString("integration.attributeplus.namespace", "RelicSystem");

        if (debugMode) {
            plugin.getLogger().info("[ConfigManager] 配置已重载");
            plugin.getLogger().info("  调试模式: " + debugMode);
            plugin.getLogger().info("  最大等级: " + maxLevel);
            plugin.getLogger().info("  存储模式: " + storageMode);
            plugin.getLogger().info("  AP集成: " + attributePlusEnabled);
        }
    }

    /**
     * 是否启用调试模式
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * 是否启用效果调试
     */
    public boolean isDebugEffects() {
        return debugEffects;
    }

    /**
     * 获取最大等级
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * 是否自动迁移数据
     */
    public boolean isAutoMigration() {
        return autoMigration;
    }

    /**
     * 获取存储模式
     */
    public String getStorageMode() {
        return storageMode;
    }

    /**
     * AttributePlus 是否启用
     */
    public boolean isAttributePlusEnabled() {
        return attributePlusEnabled;
    }

    /**
     * 获取 AttributePlus 命名空间
     */
    public String getAttributePlusNamespace() {
        return attributePlusNamespace;
    }
}

