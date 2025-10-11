package com.salteddoubao.relicsystem.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.*;

import java.io.File;
import java.util.*;

/**
 * 圣遗物管理器（M1：仅加载基础配置骨架）
 */
public class RelicManager {
    private final MinecraftRelicSystem plugin;

    private final Map<String, RelicSet> setRegistry = new HashMap<>();

    public RelicManager(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        ensureDefaultFiles();
        loadSets();
        loadAttributePool();
    }

    private void ensureDefaultFiles() {
        File dir = new File(plugin.getDataFolder(), "relics");
        if (!dir.exists()) dir.mkdirs();
        // 仅拷贝 sets.yml，其他文件后续里程碑补充
        File sets = new File(dir, "sets.yml");
        if (!sets.exists()) plugin.saveResource("relics/sets.yml", false);
        File pool = new File(dir, "attribute_pool.yml");
        if (!pool.exists()) plugin.saveResource("relics/attribute_pool.yml", false);
    }

    private void loadSets() {
        setRegistry.clear();
        File sets = new File(plugin.getDataFolder(), "relics/sets.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(sets);
        ConfigurationSection sec = cfg.getConfigurationSection("sets");
        if (sec == null) {
            plugin.getLogger().warning("relics/sets.yml 未找到 sets 节");
            return;
        }
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            String name = s.getString("name", id);
            // 模板物品，若未配置则使用默认：AMETHYST_SHARD
            org.bukkit.Material template = org.bukkit.Material.AMETHYST_SHARD;
            String mat = s.getString("template_item");
            if (mat != null && !mat.isEmpty()) {
                try {
                    template = org.bukkit.Material.valueOf(mat.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("套装 [" + id + "] 的模板材质无效: " + mat + "，使用默认材质");
                }
            }
            List<String> two = s.getStringList("bonuses.two_piece_desc");
            List<String> four = s.getStringList("bonuses.four_piece_desc");
            RelicSet rs = new RelicSet(id, name, two, four, template);
            setRegistry.put(id, rs);
        }
        plugin.getLogger().info("RelicManager: 已加载套装数量=" + setRegistry.size());
    }

    public RelicSet getRelicSet(String id) { return setRegistry.get(id); }
    public Set<String> getRelicSetIds() { return Collections.unmodifiableSet(setRegistry.keySet()); }

    // === 属性池 ===
    public static class AttributePoolConfig {
        public static class MainEntry {
            public java.util.Set<String> slots = new java.util.HashSet<>();
            public double base;
            public double step;
        }
        public static class SubEntry {
            public int weight = 1;
            public java.util.List<Double> pool = new java.util.ArrayList<>();
        }
        public final java.util.Map<String, MainEntry> mainStats = new java.util.HashMap<>();
        public final java.util.Map<String, SubEntry> subStats = new java.util.HashMap<>();
        public static class RarityRule {
            public int maxLevel;
            public int minInitial;
            public int maxInitial;
        }
        public final java.util.Map<String, RarityRule> rarityRules = new java.util.HashMap<>();
    }

    private AttributePoolConfig attributePool;

    private void loadAttributePool() {
        try {
            File f = new File(plugin.getDataFolder(), "relics/attribute_pool.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);

            AttributePoolConfig pool = new AttributePoolConfig();

            org.bukkit.configuration.ConfigurationSection mainSec = cfg.getConfigurationSection("main_stats");
            if (mainSec != null) {
                for (String key : mainSec.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection ms = mainSec.getConfigurationSection(key);
                    if (ms == null) continue;
                    AttributePoolConfig.MainEntry e = new AttributePoolConfig.MainEntry();
                    e.slots.addAll(ms.getStringList("slots"));
                    e.base = ms.getDouble("base", 0.0);
                    e.step = ms.getDouble("step", plugin.getConfig().getDouble("relic.mainstat.step_default", 1.0));
                    pool.mainStats.put(key, e);
                }
            }

            org.bukkit.configuration.ConfigurationSection subSec = cfg.getConfigurationSection("sub_stats");
            if (subSec != null) {
                for (String key : subSec.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection ss = subSec.getConfigurationSection(key);
                    if (ss == null) continue;
                    AttributePoolConfig.SubEntry e = new AttributePoolConfig.SubEntry();
                    e.weight = Math.max(0, ss.getInt("weight", 1));
                    java.util.List<Double> values = new java.util.ArrayList<>();
                    for (Object o : ss.getList("pool", java.util.Collections.emptyList())) {
                        if (o instanceof Number) values.add(((Number) o).doubleValue());
                        else {
                            try {
                                values.add(Double.parseDouble(String.valueOf(o)));
                            } catch (NumberFormatException ex) {
                                plugin.getLogger().warning("副词条 [" + key + "] 池中存在无效数值: " + o + "，已跳过");
                            }
                        }
                    }
                    e.pool.addAll(values);
                    pool.subStats.put(key, e);
                }
            }

            org.bukkit.configuration.ConfigurationSection raritySec = cfg.getConfigurationSection("rarity");
            if (raritySec != null) {
                for (String key : raritySec.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection rs = raritySec.getConfigurationSection(key);
                    if (rs == null) continue;
                    AttributePoolConfig.RarityRule r = new AttributePoolConfig.RarityRule();
                    r.maxLevel = rs.getInt("max_level", 20);
                    java.util.List<Integer> range = rs.getIntegerList("initial_substat_count");
                    if (range.size() >= 2) {
                        r.minInitial = Math.min(range.get(0), range.get(1));
                        r.maxInitial = Math.max(range.get(0), range.get(1));
                    } else if (range.size() == 1) {
                        r.minInitial = r.maxInitial = range.get(0);
                    } else {
                        r.minInitial = 1; r.maxInitial = 2;
                    }
                    pool.rarityRules.put(key, r);
                }
            }

            this.attributePool = pool;
            plugin.getLogger().info("RelicManager: 属性池加载完成 (主词条=" + pool.mainStats.size() + ", 副词条=" + pool.subStats.size() + ")");
            // 若启用 AP 集成，提示未映射的属性键（帮助服主清理不兼容键）
            try {
                com.salteddoubao.relicsystem.service.AttributePlusBridge bridge = plugin.getAttributePlusBridge();
                if (bridge != null && bridge.isEnabled()) {
                    java.util.Set<String> apKeys = new java.util.HashSet<>();
                    for (com.salteddoubao.relicsystem.relic.RelicStatType t : com.salteddoubao.relicsystem.relic.RelicStatType.values()) {
                        // 仅统计在 config.yml 中配置了映射的键
                        String mapped = plugin.getConfig().getString("integration.attributeplus.stat_map." + t.name());
                        if (mapped != null) apKeys.add(t.name());
                    }
                    java.util.List<String> unknown = new java.util.ArrayList<>();
                    for (String k : pool.mainStats.keySet()) if (!apKeys.contains(k)) unknown.add(k);
                    for (String k : pool.subStats.keySet()) if (!apKeys.contains(k)) unknown.add(k);
                    if (!unknown.isEmpty()) {
                        plugin.getLogger().warning("检测到属性池中存在未映射至 AP 的属性: " + unknown);
                    }
                }
            } catch (Exception e) {
                // AP集成检查失败，不影响主功能
                plugin.getLogger().fine("AP映射检查失败: " + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("加载属性池失败: " + e.getMessage());
        }
    }

    public AttributePoolConfig getAttributePool() { return attributePool; }
}


