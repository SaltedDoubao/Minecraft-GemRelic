package com.lymc.gemrelic.model;

import org.bukkit.Material;

/**
 * 升级配置模型类
 * 用于存储宝石升级所需的配置信息
 */
public class UpgradeConfig {
    private final Material costMaterial;
    private final int costAmount;
    private final double successChance;
    private final double growthRate;

    /**
     * 构造函数
     *
     * @param costMaterial  升级消耗的材料类型
     * @param costAmount    升级消耗的材料数量
     * @param successChance 升级成功率（0.0 - 1.0）
     * @param growthRate    属性成长倍率
     */
    public UpgradeConfig(Material costMaterial, int costAmount, 
                        double successChance, double growthRate) {
        this.costMaterial = costMaterial;
        this.costAmount = costAmount;
        this.successChance = Math.max(0.0, Math.min(1.0, successChance)); // 限制在0-1之间
        this.growthRate = growthRate;
    }

    public Material getCostMaterial() {
        return costMaterial;
    }

    public int getCostAmount() {
        return costAmount;
    }

    public double getSuccessChance() {
        return successChance;
    }

    public double getGrowthRate() {
        return growthRate;
    }

    @Override
    public String toString() {
        return "UpgradeConfig{" +
                "costMaterial=" + costMaterial +
                ", costAmount=" + costAmount +
                ", successChance=" + (successChance * 100) + '%' +
                ", growthRate=" + growthRate +
                '}';
    }
}

