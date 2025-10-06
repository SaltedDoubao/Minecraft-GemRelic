package com.salteddoubao.relicplugin.util;

import org.bukkit.configuration.ConfigurationSection;

import com.salteddoubao.relicplugin.relic.*;

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
            RelicMainStat mainStat = new RelicMainStat(
                    RelicStatType.valueOf(main.getString("type")),
                    main.getDouble("value", 0.0)
            );
            List<RelicSubstat> subs = new ArrayList<>();
            List<Map<String, Object>> list = (List<Map<String, Object>>) sec.getList("substats", Collections.emptyList());
            for (Map<String, Object> sm : list) {
                RelicStatType t = RelicStatType.valueOf(String.valueOf(sm.get("type")));
                double v = sm.get("value") instanceof Number ? ((Number) sm.get("value")).doubleValue() : 0.0;
                subs.add(new RelicSubstat(t, v));
            }
            return new RelicData(id, setId, slot, rarity, level, exp, mainStat, subs, locked);
        } catch (Exception e) {
            return null;
        }
    }
}


