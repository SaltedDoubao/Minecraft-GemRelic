package com.salteddoubao.relicsystem;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import com.salteddoubao.relicsystem.command.RelicCommand;
import com.salteddoubao.relicsystem.listener.PlayerListener;
import com.salteddoubao.relicsystem.listener.RelicGUIListener;
import com.salteddoubao.relicsystem.listener.TreasureBoxListener;
import com.salteddoubao.relicsystem.listener.CombatListener;
import com.salteddoubao.relicsystem.manager.RelicManager;
import com.salteddoubao.relicsystem.manager.TreasureBoxManager;
import com.salteddoubao.relicsystem.service.RelicEffectService;
import com.salteddoubao.relicsystem.service.RelicGenerationService;
import com.salteddoubao.relicsystem.service.StatAggregationService;
import com.salteddoubao.relicsystem.service.AttributePlusBridge;
import com.salteddoubao.relicsystem.storage.IRelicProfileManager;
import com.salteddoubao.relicsystem.storage.StorageFactory;
import com.salteddoubao.relicsystem.util.RelicItemConverter;
import com.salteddoubao.relicsystem.util.ExceptionHandler;
import com.salteddoubao.relicsystem.util.ConfigManager;

/**
 * RelicSystem 插件主类
 * 圣遗物属性系统
 * 
 * @author SaltedDoubao
 * @version 1.0.0
 */
public class MinecraftRelicSystem extends JavaPlugin {
    
    private static MinecraftRelicSystem instance;
    private RelicManager relicManager;
    private IRelicProfileManager relicProfileManager;
    private StorageFactory storageFactory;
    private RelicEffectService relicEffectService;
    private StatAggregationService statAggregationService;
    private RelicItemConverter relicItemConverter;
    private RelicGenerationService relicGenerationService;
    private TreasureBoxManager treasureBoxManager;
    private AttributePlusBridge attributePlusBridge;
    private ExceptionHandler exceptionHandler;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // 打印启动信息
        getLogger().info("==============================");
        getLogger().info("  RelicSystem 正在启动...");
        getLogger().info("  版本: " + getPluginMeta().getVersion());
        getLogger().info("  作者: SaltedDoubao");
        getLogger().info("==============================");

        // 创建数据文件夹
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 保存默认配置文件
        saveDefaultConfig();

        // 校验配置文件
        validateConfig();

        // 初始化管理器
        initializeManagers();

        // 注册命令
        registerCommands();

        // 注册事件监听器
        registerListeners();

        getLogger().info("RelicSystem 启动成功！");
    }

    @Override
    public void onDisable() {
        getLogger().info("RelicSystem 正在关闭...");
        
        // 1. 先保存所有在线玩家的数据
        if (relicProfileManager != null) {
            getLogger().info("正在保存所有玩家数据...");
            relicProfileManager.saveAll();
        }

        // 2. 再清理属性修饰（包含原版修饰与 AP 来源），避免残留
        try {
            if (relicEffectService != null) {
                getLogger().info("正在清理玩家属性...");
                for (Player p : getServer().getOnlinePlayers()) {
                    try {
                        relicEffectService.clear(p);
                    } catch (Exception e) {
                        getLogger().warning("清理玩家属性失败: " + p.getName() + " - " + e.getMessage());
                        // 继续处理其他玩家
                    }
                }
            }
        } catch (Exception e) {
            // AP未启用或清理失败，不影响插件卸载
            if (configManager != null && configManager.isDebugMode()) {
                getLogger().fine("AP属性清理跳过: " + e.getMessage());
            }
        }
        
        getLogger().info("RelicSystem 已关闭，感谢使用！");
    }

    /**
     * 校验配置文件
     */
    private void validateConfig() {
        java.util.List<String> errors = new java.util.ArrayList<>();

        // 检查存储模式
        String storageMode = getConfig().getString("relic.storage_mode");
        if (!java.util.Arrays.asList("YAML", "INVENTORY").contains(storageMode)) {
            errors.add("无效的存储模式: " + storageMode + "，有效值为: YAML, INVENTORY");
        }

        // 检查最大等级
        int maxLevel = getConfig().getInt("relic.max_level", 20);
        if (maxLevel < 0 || maxLevel > 100) {
            errors.add("无效的最大等级: " + maxLevel + "，建议范围: 0-100");
        }

        // 检查 AttributePlus 映射
        if (getConfig().getBoolean("integration.attributeplus.enabled")) {
            org.bukkit.configuration.ConfigurationSection statMap = getConfig().getConfigurationSection("integration.attributeplus.stat_map");
            if (statMap == null || statMap.getKeys(false).isEmpty()) {
                errors.add("AttributePlus 已启用但未配置 stat_map");
            }
        }

        // 检查属性池文件
        java.io.File poolFile = new java.io.File(getDataFolder(), "relics/attribute_pool.yml");
        if (!poolFile.exists()) {
            errors.add("属性池文件不存在: " + poolFile.getPath());
        }

        // 检查套装文件
        java.io.File setsFile = new java.io.File(getDataFolder(), "relics/sets.yml");
        if (!setsFile.exists()) {
            errors.add("套装文件不存在: " + setsFile.getPath());
        }

        // 输出错误信息
        if (!errors.isEmpty()) {
            getLogger().severe("==============================");
            getLogger().severe("配置文件存在错误：");
            errors.forEach(e -> getLogger().severe("  - " + e));
            getLogger().severe("插件可能无法正常工作，请检查配置！");
            getLogger().severe("==============================");
        }
    }

    /**
     * 初始化管理器
     */
    private void initializeManagers() {
        getLogger().info("正在初始化管理器...");
        
        // 初始化基础工具
        exceptionHandler = new ExceptionHandler(this);
        configManager = new ConfigManager(this);
        relicItemConverter = new RelicItemConverter(this);

        // 初始化存储系统
        storageFactory = new StorageFactory(this);
        getLogger().info("存储模式: " + storageFactory.getStorageModeDisplayName());
        getLogger().info("存储描述: " + storageFactory.getStorageModeDescription());
        
        // 初始化各个管理器
        relicManager = new RelicManager(this);
        relicProfileManager = storageFactory.createProfileManager();
        relicEffectService = new RelicEffectService(this);
        statAggregationService = new StatAggregationService();
        relicGenerationService = new RelicGenerationService(this);
        treasureBoxManager = new TreasureBoxManager(this);
        // 初始化 AttributePlus 桥接
        attributePlusBridge = new AttributePlusBridge(this);
        if (attributePlusBridge.isEnabled()) {
            getLogger().info("AttributePlus 集成已启用，命名空间=" + getConfig().getString("integration.attributeplus.namespace", "RelicSystem"));
        } else {
            getLogger().info("AttributePlus 集成未启用或未安装，将使用内置属性与战斗计算");
        }
        
        getLogger().info("管理器初始化完成！");
    }

    /**
     * 注册命令
     */
    private void registerCommands() {
        getLogger().info("正在注册命令...");
        if (getCommand("relic") != null) {
            RelicCommand relicCommand = new RelicCommand(this);
            getCommand("relic").setExecutor(relicCommand);
            getCommand("relic").setTabCompleter(relicCommand);
        }
        getLogger().info("命令注册完成！");
    }

    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getLogger().info("正在注册事件监听器...");
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new RelicGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new TreasureBoxListener(this), this);
        // 若启用 AP 集成，则禁用内置战斗监听器
        if (attributePlusBridge == null || !attributePlusBridge.isEnabled()) {
            getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        } else {
            getLogger().info("已检测到 AttributePlus，跳过注册 CombatListener，战斗属性由 AP 处理");
        }
        getLogger().info("事件监听器注册完成！");
    }

    /**
     * 获取插件实例
     *
     * @return 插件实例
     */
    public static MinecraftRelicSystem getInstance() {
        return instance;
    }

    public RelicManager getRelicManager() { return relicManager; }
    public IRelicProfileManager getRelicProfileManager() { return relicProfileManager; }
    public StorageFactory getStorageFactory() { return storageFactory; }
    public RelicEffectService getRelicEffectService() { return relicEffectService; }
    public StatAggregationService getStatAggregationService() { return statAggregationService; }
    // 已移除 AP 依赖，使用内置属性引擎
    public RelicItemConverter getRelicItemConverter() { return relicItemConverter; }
    public RelicGenerationService getRelicGenerationService() { return relicGenerationService; }
    public TreasureBoxManager getTreasureBoxManager() { return treasureBoxManager; }
    public AttributePlusBridge getAttributePlusBridge() { return attributePlusBridge; }
    public ExceptionHandler getExceptionHandler() { return exceptionHandler; }
    public ConfigManager getConfigManager() { return configManager; }

    // 仅重载圣遗物配置
    public void reloadRelicConfig() {
        // 重载配置缓存
        if (configManager != null) {
            configManager.reload();
        }
        relicManager = new RelicManager(this);
        // 重载后可能需要更新存储系统
        if (storageFactory != null) {
            getLogger().info("配置重载后的存储模式: " + storageFactory.getStorageModeDisplayName());
        }
    }
}

