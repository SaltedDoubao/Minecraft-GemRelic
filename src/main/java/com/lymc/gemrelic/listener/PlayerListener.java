package com.lymc.gemrelic.listener;

import com.lymc.gemrelic.GemRelicPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家事件监听器
 * 监听玩家相关事件，后续将扩展属性加成功能
 */
public class PlayerListener implements Listener {
    
    private final GemRelicPlugin plugin;

    public PlayerListener(GemRelicPlugin plugin) {
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
        // 暂时只发送欢迎信息，验证监听器工作正常
        // 后续扩展：
        // 1. 加载玩家的宝石数据
        // 2. 计算并缓存玩家的总属性加成
        // 3. 同步玩家装备上的宝石属性
        
        plugin.getLogger().info(event.getPlayer().getName() + " 加入服务器，监听器工作正常");
    }

    /**
     * 玩家退出服务器事件
     * 用于保存玩家数据、清理缓存等
     *
     * @param event 玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 后续扩展：
        // 1. 保存玩家的宝石数据
        // 2. 清理玩家的属性缓存
        
        plugin.getLogger().info(event.getPlayer().getName() + " 退出服务器");
    }

    // 后续扩展的事件监听器：
    // 
    // @EventHandler
    // public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    //     // 处理攻击伤害加成
    //     // 从玩家装备的宝石中读取攻击力、暴击率等属性
    //     // 应用到伤害计算中
    // }
    //
    // @EventHandler
    // public void onEntityDamage(EntityDamageEvent event) {
    //     // 处理防御加成
    //     // 从玩家装备的宝石中读取防御力等属性
    //     // 减少受到的伤害
    // }
    //
    // @EventHandler
    // public void onPlayerInteract(PlayerInteractEvent event) {
    //     // 处理宝石镶嵌/拆卸交互
    //     // 右键点击装备打开镶嵌界面
    // }
    //
    // @EventHandler
    // public void onInventoryClick(InventoryClickEvent event) {
    //     // 处理宝石镶嵌GUI的点击事件
    // }
}

