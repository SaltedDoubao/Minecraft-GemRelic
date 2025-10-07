package com.salteddoubao.relicsystem;

import org.bukkit.plugin.java.JavaPlugin;

import com.salteddoubao.relicsystem.command.RelicCommand;
import com.salteddoubao.relicsystem.listener.PlayerListener;
import com.salteddoubao.relicsystem.listener.RelicGUIListener;
import com.salteddoubao.relicsystem.listener.TreasureBoxListener;
import com.salteddoubao.relicsystem.manager.RelicManager;
import com.salteddoubao.relicsystem.manager.TreasureBoxManager;
import com.salteddoubao.relicsystem.service.AttributePlusBridge;
import com.salteddoubao.relicsystem.service.RelicEffectService;
import com.salteddoubao.relicsystem.service.RelicGenerationService;
import com.salteddoubao.relicsystem.service.StatAggregationService;
import com.salteddoubao.relicsystem.storage.IRelicProfileManager;
import com.salteddoubao.relicsystem.storage.StorageFactory;
import com.salteddoubao.relicsystem.util.RelicItemConverter;

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
    private AttributePlusBridge attributePlusBridge;
    private RelicItemConverter relicItemConverter;
    private RelicGenerationService relicGenerationService;
    private TreasureBoxManager treasureBoxManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // 打印启动信息
        getLogger().info("==============================");
        getLogger().info("  RelicSystem 正在启动...");
        getLogger().info("  版本: " + getDescription().getVersion());
        getLogger().info("  作者: SaltedDoubao");
        getLogger().info("==============================");

        // 创建数据文件夹
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 保存默认配置文件
        saveDefaultConfig();

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
        
        // 保存所有在线玩家的圣遗物数据
        if (relicProfileManager != null) {
            relicProfileManager.saveAll();
        }
        
        getLogger().info("RelicSystem 已关闭，感谢使用！");
    }

    /**
     * 初始化管理器
     */
    private void initializeManagers() {
        getLogger().info("正在初始化管理器...");
        
        // 初始化基础工具
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
        attributePlusBridge = new AttributePlusBridge(this);
        relicGenerationService = new RelicGenerationService(this);
        treasureBoxManager = new TreasureBoxManager(this);
        
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
    public AttributePlusBridge getAttributePlusBridge() { return attributePlusBridge; }
    public RelicItemConverter getRelicItemConverter() { return relicItemConverter; }
    public RelicGenerationService getRelicGenerationService() { return relicGenerationService; }
    public TreasureBoxManager getTreasureBoxManager() { return treasureBoxManager; }

    // 仅重载圣遗物配置
    public void reloadRelicConfig() {
        relicManager = new RelicManager(this);
        // 重载后可能需要更新存储系统
        if (storageFactory != null) {
            StorageFactory.StorageMode newMode = storageFactory.getStorageMode();
            getLogger().info("配置重载后的存储模式: " + storageFactory.getStorageModeDisplayName());
        }
    }
}

