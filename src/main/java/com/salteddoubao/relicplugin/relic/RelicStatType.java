package com.salteddoubao.relicplugin.relic;

/**
 * 圣遗物属性类型（示例，后续可表驱动）
 */
public enum RelicStatType {
    HP_FLAT(false, "§a生命值"),
    ATK_FLAT(false, "§c攻击力"),

    HP_PCT(true, "§a生命值%"),
    ATK_PCT(true, "§c攻击力%"),
    DEF_PCT(true, "§7防御%"),

    CRIT_RATE(false, "§e暴击率"),
    CRIT_DMG(false, "§6暴击伤害"),
    ATK_SPEED(false, "§b攻速"),
    MOVE_SPEED(false, "§b移速"),
    LUCK(false, "§d幸运"),
    KB_RES(false, "§9击退抗性"),

    HEAL_BONUS(false, "§a治疗加成"),
    ELEM_DMG_ANY(false, "§9元素伤害");

    private final boolean percent;
    private final String display;

    RelicStatType(boolean percent, String display) {
        this.percent = percent;
        this.display = display;
    }

    public boolean isPercent() { return percent; }
    public String getDisplay() { return display; }
}


