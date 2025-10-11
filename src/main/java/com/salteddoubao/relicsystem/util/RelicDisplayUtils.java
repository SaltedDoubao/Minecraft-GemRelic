package com.salteddoubao.relicsystem.util;

import org.bukkit.Material;
import com.salteddoubao.relicsystem.relic.RelicRarity;
import com.salteddoubao.relicsystem.relic.RelicSlot;

/**
 * 圣遗物显示工具类
 * 统一管理圣遗物相关的材料、颜色和显示名称映射
 */
public class RelicDisplayUtils {
    
    /**
     * 根据稀有度获取对应的材料
     */
    public static Material getRarityMaterial(RelicRarity rarity) {
        return switch (rarity) {
            case WHITE -> Material.QUARTZ;
            case GREEN -> Material.EMERALD;
            case BLUE -> Material.LAPIS_LAZULI;
            case PURPLE -> Material.AMETHYST_SHARD;
            case GOLD -> Material.GOLD_INGOT;
        };
    }
    
    /**
     * 根据稀有度获取对应的颜色代码
     */
    public static String getRarityColor(RelicRarity rarity) {
        return switch (rarity) {
            case WHITE -> "§f";
            case GREEN -> "§a";
            case BLUE -> "§9";
            case PURPLE -> "§d";
            case GOLD -> "§6";
        };
    }
    
    /**
     * 根据槽位获取对应的显示名称（中文）
     */
    public static String getSlotDisplayName(RelicSlot slot) {
        return switch (slot) {
            case FLOWER -> "生之花";
            case PLUME -> "死之羽";
            case SANDS -> "时之沙";
            case GOBLET -> "空之杯";
            case CIRCLET -> "理之冠";
        };
    }
    
    /**
     * 根据槽位获取对应的材料（用于空槽位显示）
     */
    public static Material getSlotMaterial(RelicSlot slot) {
        return switch (slot) {
            case FLOWER -> Material.BLUE_ORCHID;
            case PLUME -> Material.FEATHER;
            case SANDS -> Material.CLOCK;
            case GOBLET -> Material.POTION;
            case CIRCLET -> Material.PLAYER_HEAD;
        };
    }
}

