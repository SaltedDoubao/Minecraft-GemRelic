package com.lymc.gemrelic.model;

/**
 * 属性数据模型类
 * 用于存储宝石的属性信息
 */
public class AttributeData {
    private final String type;
    private final String name;
    private final double minValue;
    private final double maxValue;

    /**
     * 构造函数
     *
     * @param type     属性类型（如 attack, defense, crit_rate 等）
     * @param name     属性显示名称
     * @param minValue 属性最小值
     * @param maxValue 属性最大值
     */
    public AttributeData(String type, String name, double minValue, double maxValue) {
        this.type = type;
        this.name = name;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    /**
     * 生成随机属性值
     *
     * @return 在最小值和最大值之间的随机数
     */
    public double generateRandomValue() {
        return minValue + Math.random() * (maxValue - minValue);
    }

    @Override
    public String toString() {
        return "AttributeData{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", range=[" + minValue + ", " + maxValue + ']' +
                '}';
    }
}

