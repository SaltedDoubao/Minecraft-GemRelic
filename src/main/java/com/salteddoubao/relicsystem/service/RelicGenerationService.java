package com.salteddoubao.relicsystem.service;

import java.util.*;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.manager.RelicManager;
import com.salteddoubao.relicsystem.relic.*;

/**
 * 圣遗物生成与升级服务
 * - 主词条：按槽位与属性池选择，基础值固定；可选启用线性随等级提升
 * - 副词条：按品质生成1-4条；每5级新增或强化一条（不超过4条）
 */
public class RelicGenerationService {
    private final MinecraftRelicSystem plugin;
    private final Random random = new Random();

    public RelicGenerationService(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
    }

    public RelicData generate(String setId, RelicSlot slot, RelicRarity rarity, int level) {
        RelicManager.AttributePoolConfig pool = plugin.getRelicManager().getAttributePool();
        if (pool == null) throw new IllegalStateException("属性池未加载");

        int clampedLevel = Math.max(0, level);
        RelicManager.AttributePoolConfig.RarityRule rule = pool.rarityRules.getOrDefault(rarity.name(), defaultRuleFor(rarity));
        clampedLevel = Math.min(clampedLevel, rule.maxLevel);

        RelicMainStat main = rollMainStat(slot, pool, clampedLevel);
        List<RelicSubstat> substats = rollInitialSubstats(main.getType(), pool, rule);

        RelicData data = new RelicData(UUID.randomUUID(), setId, slot, rarity, 0, 0, main, substats, false);
        if (clampedLevel > 0) levelTo(data, clampedLevel, pool, rule);
        return data;
    }

    private void levelTo(RelicData data, int targetLevel, RelicManager.AttributePoolConfig pool, RelicManager.AttributePoolConfig.RarityRule rule) {
        boolean scaleMain = plugin.getConfig().getBoolean("relic.mainstat.scale", false);
        double defaultMainStep = plugin.getConfig().getDouble("relic.mainstat.step_default", 1.0);
        double mainBase = getMainBaseValue(data.getMainStat().getType().name(), pool);
        double mainStep = Optional.ofNullable(pool.mainStats.get(data.getMainStat().getType().name()))
                .map(v -> v.step)
                .orElse(defaultMainStep);

        for (int lv = 1; lv <= targetLevel; lv++) {
            data.setLevel(lv);
            if (scaleMain) {
                data.getMainStat().setValue(mainBase + lv * mainStep);
            }
            if (lv % 5 == 0) {
                if (data.substatCount() < 4) {
                    RelicStatType newType = rollWeightedSubTypeExcluding(data.getMainStat().getType(), data.getSubstats(), pool);
                    if (newType != null) {
                        double delta = rollSubValue(newType, pool);
                        data.addOrIncrementSubstat(newType, delta);
                        continue;
                    }
                }
                // 强化已有一条
                List<RelicSubstat> subs = data.getSubstats();
                if (!subs.isEmpty()) {
                    RelicSubstat choice = subs.get(random.nextInt(subs.size()));
                    double delta = rollSubValue(choice.getType(), pool);
                    data.addOrIncrementSubstat(choice.getType(), delta);
                }
            }
        }
    }

    private RelicMainStat rollMainStat(RelicSlot slot, RelicManager.AttributePoolConfig pool, int level) {
        RelicStatType type;
        if (slot == RelicSlot.FLOWER) {
            type = RelicStatType.HP_FLAT;
        } else if (slot == RelicSlot.PLUME) {
            type = RelicStatType.ATK_FLAT;
        } else {
            List<RelicStatType> candidates = new ArrayList<>();
            for (Map.Entry<String, RelicManager.AttributePoolConfig.MainEntry> e : pool.mainStats.entrySet()) {
                try {
                    RelicStatType t = RelicStatType.valueOf(e.getKey());
                    if (e.getValue().slots.contains(slot.name())) candidates.add(t);
                } catch (Exception ignore) {}
            }
            if (candidates.isEmpty()) type = RelicStatType.ATK_PCT; else type = candidates.get(random.nextInt(candidates.size()));
        }

        double base = getMainBaseValue(type.name(), pool);
        boolean scaleMain = plugin.getConfig().getBoolean("relic.mainstat.scale", false);
        double defaultMainStep = plugin.getConfig().getDouble("relic.mainstat.step_default", 1.0);
        double step = Optional.ofNullable(pool.mainStats.get(type.name())).map(v -> v.step).orElse(defaultMainStep);
        double value = scaleMain ? base + level * step : base;
        return new RelicMainStat(type, value);
    }

    private double getMainBaseValue(String typeKey, RelicManager.AttributePoolConfig pool) {
        RelicManager.AttributePoolConfig.MainEntry e = pool.mainStats.get(typeKey);
        return e != null ? e.base : 0.0;
    }

    private List<RelicSubstat> rollInitialSubstats(RelicStatType main, RelicManager.AttributePoolConfig pool, RelicManager.AttributePoolConfig.RarityRule rule) {
        int min = Math.max(0, rule.minInitial);
        int max = Math.max(min, rule.maxInitial);
        int count = randomInt(min, max);
        List<RelicSubstat> result = new ArrayList<>();
        Set<RelicStatType> used = new HashSet<>();
        used.add(main);
        while (result.size() < count) {
            RelicStatType t = rollWeightedSubTypeExcluding(main, result, pool);
            if (t == null) break;
            used.add(t);
            result.add(new RelicSubstat(t, rollSubValue(t, pool)));
        }
        return result;
    }

    private RelicStatType rollWeightedSubTypeExcluding(RelicStatType main, List<RelicSubstat> existing, RelicManager.AttributePoolConfig pool) {
        Set<RelicStatType> exclude = new HashSet<>();
        exclude.add(main);
        for (RelicSubstat s : existing) exclude.add(s.getType());

        int total = 0;
        List<Map.Entry<RelicStatType, Integer>> bag = new ArrayList<>();
        for (Map.Entry<String, RelicManager.AttributePoolConfig.SubEntry> e : pool.subStats.entrySet()) {
            try {
                RelicStatType t = RelicStatType.valueOf(e.getKey());
                if (exclude.contains(t)) continue;
                int w = Math.max(0, e.getValue().weight);
                if (w <= 0) continue;
                bag.add(Map.entry(t, w));
                total += w;
            } catch (Exception ignore) {}
        }
        if (total <= 0 || bag.isEmpty()) return null;
        int r = random.nextInt(total) + 1;
        int acc = 0;
        for (Map.Entry<RelicStatType, Integer> e : bag) {
            acc += e.getValue();
            if (r <= acc) return e.getKey();
        }
        return bag.get(bag.size() - 1).getKey();
    }

    private double rollSubValue(RelicStatType type, RelicManager.AttributePoolConfig pool) {
        RelicManager.AttributePoolConfig.SubEntry e = pool.subStats.get(type.name());
        if (e == null || e.pool.isEmpty()) return 0.0;
        return e.pool.get(random.nextInt(e.pool.size()));
    }

    private int randomInt(int a, int b) {
        if (a == b) return a;
        if (a > b) { int t = a; a = b; b = t; }
        return a + random.nextInt(b - a + 1);
    }

    private RelicManager.AttributePoolConfig.RarityRule defaultRuleFor(RelicRarity rarity) {
        RelicManager.AttributePoolConfig.RarityRule r = new RelicManager.AttributePoolConfig.RarityRule();
        r.maxLevel = rarity.getDefaultMaxLevel();
        switch (rarity) {
            case WHITE -> { r.minInitial = 1; r.maxInitial = 2; }
            case GREEN -> { r.minInitial = 1; r.maxInitial = 2; }
            case BLUE -> { r.minInitial = 2; r.maxInitial = 3; }
            case PURPLE -> { r.minInitial = 2; r.maxInitial = 3; }
            case GOLD -> { r.minInitial = 3; r.maxInitial = 4; }
            default -> { r.minInitial = 1; r.maxInitial = 2; }
        }
        return r;
    }
}


