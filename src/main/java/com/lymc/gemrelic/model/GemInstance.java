package com.lymc.gemrelic.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 宝石实例类
 * 用于表示一个具体的宝石物品实例，包含实际的属性值和等级
 */
public class GemInstance {
    private final String gemType;
    private int level;
    private final Map<String, Double> attributes;

    /**
     * 构造函数
     *
     * @param gemType    宝石类型ID
     * @param level      宝石等级
     * @param attributes 属性映射表（属性类型 -> 属性值）
     */
    public GemInstance(String gemType, int level, Map<String, Double> attributes) {
        this.gemType = gemType;
        this.level = level;
        this.attributes = new HashMap<>(attributes);
    }

    public String getGemType() {
        return gemType;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Map<String, Double> getAttributes() {
        return new HashMap<>(attributes);
    }

    /**
     * 获取指定类型的属性值
     *
     * @param attributeType 属性类型
     * @return 属性值，如果不存在则返回0.0
     */
    public double getAttribute(String attributeType) {
        return attributes.getOrDefault(attributeType, 0.0);
    }

    /**
     * 设置指定类型的属性值
     *
     * @param attributeType 属性类型
     * @param value         属性值
     */
    public void setAttribute(String attributeType, double value) {
        attributes.put(attributeType, value);
    }

    /**
     * 升级宝石属性
     *
     * @param growthRate 成长倍率
     */
    public void upgradeAttributes(double growthRate) {
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            entry.setValue(entry.getValue() * growthRate);
        }
        level++;
    }

    @Override
    public String toString() {
        return "GemInstance{" +
                "gemType='" + gemType + '\'' +
                ", level=" + level +
                ", attributes=" + attributes +
                '}';
    }
}

