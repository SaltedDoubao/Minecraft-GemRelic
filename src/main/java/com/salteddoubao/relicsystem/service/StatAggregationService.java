package com.salteddoubao.relicsystem.service;

import java.util.EnumMap;
import java.util.Map;

import com.salteddoubao.relicsystem.relic.PlayerRelicProfile;
import com.salteddoubao.relicsystem.relic.RelicData;
import com.salteddoubao.relicsystem.relic.RelicStatType;
import com.salteddoubao.relicsystem.relic.RelicSubstat;

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


