package com.salteddoubao.relicsystem.relic;

/**
 * 圣遗物属性类型（示例，后续可表驱动）
 */
public enum RelicStatType {
    HP_FLAT(false, "§a生命值"),
    ATK_FLAT(false, "§c攻击力"),

    HP_PCT(true, "§a生命值%"),
    ATK_PCT(true, "§c攻击力%"),
    DEF_PCT(true, "§7防御%"),

    CRIT_RATE(true, "§e暴击率"),
    CRIT_DMG(true, "§6暴击伤害"),
    MOVE_SPEED(true, "§b移速"),

    HEAL_BONUS(true, "§a治疗加成"),

    // === AP 战斗效果（几率/倍率/额外伤害等） ===
    FIRE_CHANCE(true, "§6燃烧几率"),
    FIRE_DAMAGE(false, "§6燃烧伤害"),
    VAMPIRE_CHANCE(true, "§c吸血几率"),
    VAMPIRE_RATE(true, "§c吸血倍率"),
    HIT(true, "§3命中几率"),
    FROZEN_CHANCE(true, "§3冰冻几率"),
    FROZEN_INTENSITY(true, "§3冰冻强度"),
    LIGHTNING_CHANCE(true, "§3雷击几率"),
    LIGHTNING_DAMAGE(false, "§3雷击伤害"),
    SUNDER_ARMOR(true, "§3破甲几率"),
    SEE_THROUGH(true, "§6护甲穿透"),
    BREAK_SHIELD(true, "§3破盾几率"),

    // === 防御与抗性 ===
    ARMOR(true, "§a护甲值"),
    DEFENSE(false, "§a物理防御"),
    PVP_DEFENSE(false, "§aPVP防御"),
    PVE_DEFENSE(false, "§aPVE防御"),
    CRIT_RESIST(true, "§c暴击抗性"),
    VAMPIRE_RESIST(true, "§c吸血抗性"),
    REFLECTION_CHANCE(true, "§6反弹几率"),
    REFLECTION_RATE(true, "§6反弹倍率"),
    DODGE_CHANCE(true, "§6闪避几率"),
    SHIELD_BLOCK_CHANCE(true, "§6盾牌格挡"),
    REMOTE_IMMUNE(true, "§6箭伤免疫");

    private final boolean percent;
    private final String display;

    RelicStatType(boolean percent, String display) {
        this.percent = percent;
        this.display = display;
    }

    public boolean isPercent() { return percent; }
    public String getDisplay() { return display; }
}


