package com.lymc.gemrelic.service;

import com.lymc.gemrelic.relic.PlayerRelicProfile;
import com.lymc.gemrelic.relic.RelicData;
import com.lymc.gemrelic.relic.RelicSubstat;
import com.lymc.gemrelic.relic.RelicStatType;

import java.util.EnumMap;
import java.util.Map;

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


