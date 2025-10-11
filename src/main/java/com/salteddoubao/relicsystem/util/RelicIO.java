package com.salteddoubao.relicsystem.util;

import org.bukkit.configuration.ConfigurationSection;

import com.salteddoubao.relicsystem.relic.*;

import java.util.*;

public class RelicIO {
    public static Map<String, Object> serializeRelic(RelicData r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId().toString());
        m.put("setId", r.getSetId());
        m.put("slot", r.getSlot().name());
        m.put("rarity", r.getRarity().name());
        m.put("level", r.getLevel());
        m.put("exp", r.getExp());
        m.put("locked", r.isLocked());
        Map<String, Object> main = new LinkedHashMap<>();
        main.put("type", r.getMainStat().getType().name());
        main.put("value", r.getMainStat().getValue());
        m.put("main", main);
        List<Map<String, Object>> subs = new ArrayList<>();
        for (RelicSubstat s : r.getSubstats()) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("type", s.getType().name());
            sm.put("value", s.getValue());
            subs.add(sm);
        }
        m.put("substats", subs);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static RelicData deserializeRelic(ConfigurationSection sec) {
        try {
            UUID id = UUID.fromString(sec.getString("id"));
            String setId = sec.getString("setId");
            RelicSlot slot = RelicSlot.valueOf(sec.getString("slot"));
            RelicRarity rarity = RelicRarity.valueOf(sec.getString("rarity"));
            int level = sec.getInt("level", 0);
            int exp = sec.getInt("exp", 0);
            boolean locked = sec.getBoolean("locked", false);
            ConfigurationSection main = sec.getConfigurationSection("main");
            String mainTypeStr = main.getString("type");
            // 兼容性映射：旧枚举名 -> 新枚举名
            mainTypeStr = migrateOldStatType(mainTypeStr);
            RelicMainStat mainStat = new RelicMainStat(
                    RelicStatType.valueOf(mainTypeStr),
                    main.getDouble("value", 0.0)
            );
            List<RelicSubstat> subs = new ArrayList<>();
            List<Map<String, Object>> list = (List<Map<String, Object>>) sec.getList("substats", Collections.emptyList());
            for (Map<String, Object> sm : list) {
                String typeStr = String.valueOf(sm.get("type"));
                typeStr = migrateOldStatType(typeStr);
                RelicStatType t = RelicStatType.valueOf(typeStr);
                double v = sm.get("value") instanceof Number ? ((Number) sm.get("value")).doubleValue() : 0.0;
                subs.add(new RelicSubstat(t, v));
            }
            return new RelicData(id, setId, slot, rarity, level, exp, mainStat, subs, locked);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 迁移旧的属性类型名到新名称（兼容性映射）
     */
    private static String migrateOldStatType(String oldType) {
        if (oldType == null) return oldType;
        // 对应 AP 官方属性重命名
        return switch (oldType) {
            case "CRIT_RATE" -> "CRIT_CHANCE";  // 暴击率 -> 暴击几率
            case "CRIT_DMG" -> "CRIT_RATE";     // 暴击伤害 -> 暴伤倍率
            case "HEAL_BONUS" -> "RESTORE_RATIO"; // 治疗加成 -> 百分比恢复
            case "DEFENSE" -> "DEF_FLAT";       // 防御 -> 物理防御（平添）
            case "PVP_DEFENSE" -> "PVP_DEF";
            case "PVE_DEFENSE" -> "PVE_DEF";
            case "SHIELD_BLOCK_CHANCE" -> "SHIELD_BLOCK";
            // 已删除的属性跳过
            case "ELEM_DMG_ANY", "ATK_SPEED", "LUCK", "KB_RES" -> null;
            default -> oldType;
        };
    }
}


