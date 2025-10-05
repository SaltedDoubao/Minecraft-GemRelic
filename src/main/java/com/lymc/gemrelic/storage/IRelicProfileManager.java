package com.lymc.gemrelic.storage;

import com.lymc.gemrelic.relic.PlayerRelicProfile;
import org.bukkit.entity.Player;

/**
 * 圣遗物档案管理器接口
 * 为不同的存储方式提供统一的接口
 */
public interface IRelicProfileManager {
    
    /**
     * 获取玩家圣遗物档案
     */
    PlayerRelicProfile get(Player player);
    
    /**
     * 清理玩家档案缓存
     */
    void clear(Player player);
    
    /**
     * 保存玩家档案
     */
    void save(Player player);
    
    /**
     * 保存所有在线玩家数据
     */
    void saveAll();
}
