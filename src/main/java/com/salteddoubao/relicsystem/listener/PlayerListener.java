package com.salteddoubao.relicsystem.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.PlayerRelicProfile;
import com.salteddoubao.relicsystem.storage.IRelicProfileManager;

/**
 * 玩家事件监听器
 * 监听玩家相关事件，后续将扩展属性加成功能
 */
public class PlayerListener implements Listener {
    
    private final MinecraftRelicSystem plugin;

    public PlayerListener(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * 玩家加入服务器事件
     * 用于初始化玩家数据、缓存等
     *
     * @param event 玩家加入事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 执行自动迁移（如果需要）
        plugin.getStorageFactory().performAutoMigration(event.getPlayer());
        
        // 加载玩家圣遗物档案
        IRelicProfileManager pm = plugin.getRelicProfileManager();
        PlayerRelicProfile profile = pm.get(event.getPlayer());
        
        // 应用套装效果
        plugin.getRelicEffectService().refresh(event.getPlayer(), profile);
        plugin.getLogger().info("已加载圣遗物档案: " + event.getPlayer().getName() + " 装备件数=" + profile.getEquipped().size());
    }

    /**
     * 玩家退出服务器事件
     * 用于保存玩家数据、清理缓存等
     *
     * @param event 玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 保存并清理
        // 清理修饰并保存
        plugin.getRelicEffectService().clear(event.getPlayer());
        // 若启用 AP 集成，额外确保清理 AP 侧属性
        if (plugin.getAttributePlusBridge() != null && plugin.getAttributePlusBridge().isEnabled()) {
            try {
                plugin.getAttributePlusBridge().clear(event.getPlayer());
            } catch (Exception e) {
                if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("清理AP属性失败: " + event.getPlayer().getName() + " - " + e.getMessage());
                }
            }
        }
        plugin.getRelicProfileManager().clear(event.getPlayer());
        plugin.getLogger().info(event.getPlayer().getName() + " 退出服务器，已保存圣遗物档案");
    }
}

