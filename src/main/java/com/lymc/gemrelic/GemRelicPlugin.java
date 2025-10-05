package com.lymc.gemrelic;

import com.lymc.gemrelic.listener.PlayerListener;
import com.lymc.gemrelic.manager.RelicManager;
import com.lymc.gemrelic.command.RelicCommand;
import com.lymc.gemrelic.listener.RelicGUIListener;
import com.lymc.gemrelic.manager.RelicProfileManager;
import com.lymc.gemrelic.service.RelicEffectService;
import com.lymc.gemrelic.service.StatAggregationService;
import com.lymc.gemrelic.service.AttributePlusBridge;
import com.lymc.gemrelic.util.RelicItemConverter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GemRelic 插件主类
 * 圣遗物属性系统
 * 
 * @author LYMC
 * @version 1.0.0
 */
public class GemRelicPlugin extends JavaPlugin {
    
    private static GemRelicPlugin instance;
    private RelicManager relicManager;
    private RelicProfileManager relicProfileManager;
    private RelicEffectService relicEffectService;
    private StatAggregationService statAggregationService;
    private AttributePlusBridge attributePlusBridge;
    private RelicItemConverter relicItemConverter;

    @Override
    public void onEnable() {
        instance = this;
        
        // 打印启动信息
        getLogger().info("==============================");
        getLogger().info("  GemRelic 圣遗物系统 正在启动...");
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

        getLogger().info("GemRelic 启动成功！");
    }

    @Override
    public void onDisable() {
        getLogger().info("GemRelic 已关闭，感谢使用！");
    }

    /**
     * 初始化管理器
     */
    private void initializeManagers() {
        getLogger().info("正在初始化管理器...");
        relicManager = new RelicManager(this);
        relicProfileManager = new RelicProfileManager(this);
        relicEffectService = new RelicEffectService(this);
        statAggregationService = new StatAggregationService();
        attributePlusBridge = new AttributePlusBridge(this);
        relicItemConverter = new RelicItemConverter(this);
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
        getLogger().info("事件监听器注册完成！");
    }

    /**
     * 获取插件实例
     *
     * @return 插件实例
     */
    public static GemRelicPlugin getInstance() {
        return instance;
    }

    public RelicManager getRelicManager() { return relicManager; }
    public RelicProfileManager getRelicProfileManager() { return relicProfileManager; }
    public RelicEffectService getRelicEffectService() { return relicEffectService; }
    public StatAggregationService getStatAggregationService() { return statAggregationService; }
    public AttributePlusBridge getAttributePlusBridge() { return attributePlusBridge; }
    public RelicItemConverter getRelicItemConverter() { return relicItemConverter; }

    // 仅重载圣遗物配置
    public void reloadRelicConfig() {
        relicManager = new RelicManager(this);
    }
}

