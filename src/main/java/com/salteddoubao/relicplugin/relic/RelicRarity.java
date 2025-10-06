package com.salteddoubao.relicplugin.relic;

/**
 * 圣遗物稀有度（对应等级上限通过配置覆盖）
 */
public enum RelicRarity {
    WHITE(1, 10),
    GREEN(2, 15),
    BLUE(3, 20),
    PURPLE(4, 25),
    GOLD(5, 30);

    private final int stars;
    private final int defaultMaxLevel;

    RelicRarity(int stars, int defaultMaxLevel) {
        this.stars = stars;
        this.defaultMaxLevel = defaultMaxLevel;
    }

    public int getStars() { return stars; }

    public int getDefaultMaxLevel() { return defaultMaxLevel; }
}


