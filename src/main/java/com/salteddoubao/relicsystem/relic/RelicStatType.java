package com.salteddoubao.relicsystem.relic;

/**
 * 圣遗物属性类型（示例，后续可表驱动）
 */
public enum RelicStatType {
    // === 基础属性（对应 AP lang/zh_CN.yml 29-40 行）===
    HP_FLAT(false, "§a生命值"),
    ATK_FLAT(false, "§c攻击力"),
    DEF_FLAT(false, "§7防御"),
    
    HP_PCT(true, "§a生命值%"),
    ATK_PCT(true, "§c攻击力%"),
    DEF_PCT(true, "§7防御%"),

    // === 攻击属性（对应 AP lang/zh_CN.yml 29-31, 41-71）===
    PVP_ATK(true, "§cPVP伤害"),
    PVE_ATK(true, "§cPVE伤害"),
    REAL_ATK(false, "§6真实伤害"),
    
    CRIT_CHANCE(true, "§e暴击几率"),
    CRIT_RATE(true, "§6暴伤倍率"),
    
    VAMPIRE_CHANCE(true, "§c吸血几率"),
    VAMPIRE_RATE(true, "§c吸血倍率"),
    
    FIRE_CHANCE(true, "§6燃烧几率"),
    FIRE_DAMAGE(false, "§6燃烧伤害"),
    
    HIT(true, "§3命中几率"),
    
    FROZEN_CHANCE(true, "§3冰冻几率"),
    FROZEN_INTENSITY(true, "§3冰冻强度"),
    
    LIGHTNING_CHANCE(true, "§3雷击几率"),
    LIGHTNING_DAMAGE(false, "§3雷击伤害"),
    
    SUNDER_ARMOR(true, "§3破甲几率"),
    SEE_THROUGH(true, "§6护甲穿透"),
    BREAK_SHIELD(true, "§3破盾几率"),

    // === 防御属性（对应 AP lang/zh_CN.yml 34-36, 38, 43-51）===
    PVP_DEF(false, "§aPVP防御"),
    PVE_DEF(false, "§aPVE防御"),
    
    ARMOR(true, "§a护甲值"),
    
    CRIT_RESIST(true, "§c暴击抵抗"),
    VAMPIRE_RESIST(true, "§c吸血抵抗"),
    
    REFLECTION_CHANCE(true, "§6反弹几率"),
    REFLECTION_RATE(true, "§6反弹倍率"),
    
    DODGE_CHANCE(true, "§6闪避几率"),
    SHIELD_BLOCK(true, "§6盾牌格挡率"),
    REMOTE_IMMUNE(true, "§6箭伤免疫率"),

    // === 其他属性（对应 AP lang/zh_CN.yml 39-40, 58-59）===
    RESTORE(false, "§a生命恢复"),
    RESTORE_RATIO(true, "§a百分比恢复"),
    MOVE_SPEED(true, "§b移速加成");

    private final boolean percent;
    private final String display;

    RelicStatType(boolean percent, String display) {
        this.percent = percent;
        this.display = display;
    }

    public boolean isPercent() { return percent; }
    public String getDisplay() { return display; }
}


