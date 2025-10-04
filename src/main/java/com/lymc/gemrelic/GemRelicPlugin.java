package com.lymc.gemrelic;

import com.lymc.gemrelic.command.GemCommand;
import com.lymc.gemrelic.listener.PlayerListener;
import com.lymc.gemrelic.manager.GemManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GemRelic 插件主类
 * 宝石镶嵌与圣遗物属性系统
 * 
 * @author LYMC
 * @version 1.0.0
 */
public class GemRelicPlugin extends JavaPlugin {
    
    private static GemRelicPlugin instance;
    private GemManager gemManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // 打印启动信息
        getLogger().info("==============================");
        getLogger().info("  GemRelic 正在启动...");
        getLogger().info("  版本: " + getDescription().getVersion());
        getLogger().info("  作者: LYMC");
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
        gemManager = new GemManager(this);
        getLogger().info("管理器初始化完成！");
    }

    /**
     * 注册命令
     */
    private void registerCommands() {
        getLogger().info("正在注册命令...");
        GemCommand gemCommand = new GemCommand(this);
        getCommand("gemrelic").setExecutor(gemCommand);
        getCommand("gemrelic").setTabCompleter(gemCommand);
        getLogger().info("命令注册完成！");
    }

    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getLogger().info("正在注册事件监听器...");
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
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

    /**
     * 获取宝石管理器
     *
     * @return 宝石管理器
     */
    public GemManager getGemManager() {
        return gemManager;
    }

    /**
     * 重载插件配置
     */
    public void reloadPluginConfig() {
        reloadConfig();
        gemManager.reload();
        getLogger().info("配置已重载！");
    }
}

