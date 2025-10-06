package com.salteddoubao.relicplugin.service;

import java.util.EnumMap;
import java.util.Map;

import com.salteddoubao.relicplugin.relic.PlayerRelicProfile;
import com.salteddoubao.relicplugin.relic.RelicData;
import com.salteddoubao.relicplugin.relic.RelicStatType;
import com.salteddoubao.relicplugin.relic.RelicSubstat;

public class StatAggregationService {
    public Map<RelicStatType, Double> aggregate(PlayerRelicProfile profile) {
        Map<RelicStatType, Double> sum = new EnumMap<>(RelicStatType.class);
        for (RelicData r : profile.getEquipped().values()) {
            sum.merge(r.getMainStat().getType(), r.getMainStat().getValue(), Double::sum);
            for (RelicSubstat s : r.getSubstats()) {
                sum.merge(s.getType(), s.getValue(), Double::sum);
            }
        }
        return sum;
    }
}


