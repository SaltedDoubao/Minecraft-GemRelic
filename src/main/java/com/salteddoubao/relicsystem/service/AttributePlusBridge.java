package com.salteddoubao.relicsystem.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.RelicStatType;

import java.text.DecimalFormat;
import java.util.*;

// AP API 引入（编译期可选，运行期需软依赖存在）
// 通过反编译确认的方法：AttributeAPI.getAttrData / createStaticAttributeSource / addSourceAttribute / takeSourceAttribute
// 通过反射调用 AP API，避免编译期强依赖

public class AttributePlusBridge {
    private final MinecraftRelicSystem plugin;
    private final boolean enabled;
    private final String namespace;
    private final Map<RelicStatType, String> map = new HashMap<>();
    // 模式控制
    private final boolean apiMode;
    private final Set<String> percentageKeys;
    private final double percentScale;
    // 命令模式（回退）
    private final boolean commandModeEnabled;
    private final List<String> applyEachTemplates;
    private final List<String> clearAllTemplates;
    private final Map<UUID, Map<String, Double>> lastApplied = new HashMap<>();
    private final DecimalFormat numberFormat = new DecimalFormat("0.####");

    public AttributePlusBridge(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        boolean installed = Bukkit.getPluginManager().getPlugin("AttributePlus") != null;
        this.enabled = plugin.getConfig().getBoolean("integration.attributeplus.enabled", false) && installed;
        this.namespace = plugin.getConfig().getString("integration.attributeplus.namespace", "RelicSystem");
        for (RelicStatType t : RelicStatType.values()) {
            String key = plugin.getConfig().getString("integration.attributeplus.stat_map." + t.name());
            if (key != null) map.put(t, key);
        }
        this.apiMode = plugin.getConfig().getBoolean("integration.attributeplus.api_mode", true);
        this.percentScale = plugin.getConfig().getDouble("integration.attributeplus.percent_scale", 1.0);
        this.percentageKeys = new java.util.HashSet<>(plugin.getConfig().getStringList("integration.attributeplus.percentage_keys"));
        // 命令模式读取（回退）
        this.commandModeEnabled = plugin.getConfig().getBoolean("integration.attributeplus.command_mode.enabled", true);
        this.applyEachTemplates = new ArrayList<>(plugin.getConfig().getStringList("integration.attributeplus.command_mode.apply_each"));
        this.clearAllTemplates = new ArrayList<>(plugin.getConfig().getStringList("integration.attributeplus.command_mode.clear_all"));
        
        // 启动时输出配置状态
        if (enabled) {
            plugin.getLogger().info("[AP Bridge] AttributePlus 集成已启用 - namespace=" + namespace + ", apiMode=" + apiMode + ", stat_map映射数=" + map.size());
        }
    }

    public boolean isEnabled() { return enabled; }

    public boolean apply(Player player, Map<RelicStatType, Double> stats) {
        if (!enabled) return false;
        boolean debug = false;
        try { debug = plugin.getConfig().getBoolean("settings.debug", false); } catch (Throwable ignore) {}

        // 将圣遗物词条按"数值属性/百分比属性"分别汇总，同一 AP 键进行合并，避免覆盖
        HashMap<String, Number[]> valueMap = new HashMap<>(); // key -> [flatValue, 0]
        HashMap<String, Double> pctMap = new HashMap<>();     // key -> percentValue (已按 percentScale 缩放)

        // 优先解析 API 类以转换占位键为服务器键（中文）
        Class<?> apiClassForKey = null;
        try { apiClassForKey = Class.forName("org.serverct.ersha.api.AttributeAPI"); } catch (Throwable ignore) {}
        for (Map.Entry<RelicStatType, Double> entry : stats.entrySet()) {
            RelicStatType type = entry.getKey();
            double raw = entry.getValue();
            if (raw == 0) continue;
            String apKey = map.get(type);
            if (apKey == null || apKey.isEmpty()) continue;
            
            // 必须使用中文服务器键（AP 的 attributeNameList 使用中文键索引）
            String serverKey = resolveServerKey(null, apKey);
            boolean pctByType = false;
            try { pctByType = type.name().endsWith("_PCT"); } catch (Throwable ignore) {}
            boolean pctByKey = (percentageKeys != null && percentageKeys.contains(apKey));
            boolean asPercent = pctByType || pctByKey;

            if (asPercent) {
                double add = raw * percentScale;
                pctMap.merge(serverKey, add, Double::sum);
            } else {
                Number[] arr = valueMap.get(serverKey);
                double current = (arr != null ? arr[0].doubleValue() : 0.0) + raw;
                valueMap.put(serverKey, new Number[]{ current, 0 });
            }
        }

        // 若无任何可下发键，视为"无需变更"，返回成功以避免回退到内置引擎
        if (valueMap.isEmpty() && pctMap.isEmpty()) {
            if (debug) plugin.getLogger().info("[AP] 无可下发属性，跳过并视为成功");
            return true;
        }

        // 输出详细的下发内容（包含展开的 Number[] 数组）
        plugin.getLogger().info("[AP] 准备下发 " + player.getName());
        for (Map.Entry<String, Number[]> e : valueMap.entrySet()) {
            plugin.getLogger().info("  valueMap[" + e.getKey() + "] = [" + e.getValue()[0] + ", " + e.getValue()[1] + "]");
        }
        for (Map.Entry<String, Double> e : pctMap.entrySet()) {
            plugin.getLogger().info("  pctMap[" + e.getKey() + "] = " + e.getValue());
        }

        // 优先 API 模式
        if (apiMode) {
            try {
                Class<?> apiClass = Class.forName("org.serverct.ersha.api.AttributeAPI");
                Object attributeData = apiClass.getMethod("getAttrData", org.bukkit.entity.LivingEntity.class).invoke(null, player);
                Object attributeSource = apiClass.getMethod(
                        "createStaticAttributeSource",
                        java.util.HashMap.class, java.util.HashMap.class
                ).invoke(null, valueMap, pctMap);

                Class<?> attributeDataClass = Class.forName("org.serverct.ersha.attribute.data.AttributeData");
                Class<?> attributeSourceClass = Class.forName("org.serverct.ersha.attribute.data.AttributeSource");
                apiClass.getMethod("addSourceAttribute", attributeDataClass, String.class, attributeSourceClass)
                        .invoke(null, attributeData, namespace, attributeSource);
                
                plugin.getLogger().info("[AP] addSourceAttribute 调用成功");

                // 记录一次已应用键集合（用于命令模式清理时的参考）
                HashMap<String, Double> appliedJoin = new HashMap<>();
                for (Map.Entry<String, Number[]> e : valueMap.entrySet()) {
                    appliedJoin.merge(e.getKey(), e.getValue()[0].doubleValue(), Double::sum);
                }
                for (Map.Entry<String, Double> e : pctMap.entrySet()) {
                    appliedJoin.merge(e.getKey(), e.getValue(), Double::sum);
                }
                lastApplied.put(player.getUniqueId(), appliedJoin);
                // 触发 AP 重算，确保 /ap stats 面板即时刷新
                try {
                    Class<?> apiClass2 = Class.forName("org.serverct.ersha.api.AttributeAPI");
                    apiClass2.getMethod("updateAttribute", org.bukkit.entity.LivingEntity.class).invoke(null, player);
                } catch (Throwable ignore) {}
                return !(appliedJoin.isEmpty());
            } catch (Throwable apiError) {
                plugin.getLogger().warning("[AP] API 模式调用失败，回退命令模式: " + apiError.getMessage());
            }
        }

        // 命令模式回退
        if (commandModeEnabled && !applyEachTemplates.isEmpty()) {
            // 命令模式无法区分“数值/百分比”两类，尽力合并为一个数值下发
            HashMap<String, Double> translatedForCmd = new HashMap<>();
            for (Map.Entry<String, Number[]> e : valueMap.entrySet()) {
                translatedForCmd.merge(e.getKey(), e.getValue()[0].doubleValue(), Double::sum);
            }
            for (Map.Entry<String, Double> e : pctMap.entrySet()) {
                translatedForCmd.merge(e.getKey(), e.getValue(), Double::sum);
            }
            for (Map.Entry<String, Double> e : translatedForCmd.entrySet()) {
                String key = e.getKey();
                double value = e.getValue();
                for (String tpl : applyEachTemplates) {
                    String cmd = tpl
                            .replace("%player%", player.getName())
                            .replace("%uuid%", player.getUniqueId().toString())
                            .replace("%namespace%", namespace)
                            .replace("%key%", key)
                            .replace("%value%", numberFormat.format(value));
                    boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    if (!ok) {
                        plugin.getLogger().warning("[AP] 命令执行失败, 请检查 AttributePlus 指令模板是否正确: " + cmd);
                    }
                }
            }
            lastApplied.put(player.getUniqueId(), translatedForCmd);
            // 命令模式下同样请求一次重算
            try {
                Class<?> apiClass2 = Class.forName("org.serverct.ersha.api.AttributeAPI");
                apiClass2.getMethod("updateAttribute", org.bukkit.entity.LivingEntity.class).invoke(null, player);
            } catch (Throwable ignore) {}
            return !translatedForCmd.isEmpty();
        }

        return false;
    }

    private String resolveServerKey(Class<?> apiClassForKey, String apKey) {
        // AP 的 getServerAttributeName 接收默认属性名（中文，如"物理伤害"）返回服务器键（也是中文）
        // 我们的 config.yml 中 stat_map 存的是占位键（英文，如 attack），需要查找对应的中文默认名
        // 由于 AP attribute.yml 中 key.attackOrDefense.attack="物理伤害"，我们需要直接用中文映射
        // 方案：在 config.yml 的 stat_map 中直接配置中文服务器键，或在此处硬编码映射表
        
        // 硬编码英文占位键到中文服务器键的映射（基于 attribute.yml）
        Map<String, String> placeholderToChinese = new HashMap<>();
        placeholderToChinese.put("health", "生命力");
        placeholderToChinese.put("attack", "物理伤害");
        placeholderToChinese.put("defense", "物理防御");
        placeholderToChinese.put("pvp_attack", "PVP伤害");
        placeholderToChinese.put("pve_attack", "PVE伤害");
        placeholderToChinese.put("pvp_defense", "PVP防御");
        placeholderToChinese.put("pve_defense", "PVE防御");
        placeholderToChinese.put("real_attack", "真实伤害");
        placeholderToChinese.put("crit", "暴击几率");
        placeholderToChinese.put("crit_rate", "暴伤倍率");
        placeholderToChinese.put("vampire", "吸血几率");
        placeholderToChinese.put("vampire_rate", "吸血倍率");
        placeholderToChinese.put("shoot_speed", "箭矢速度");
        placeholderToChinese.put("shoot_spread", "箭术精准");
        placeholderToChinese.put("shoot_through", "箭矢穿透率");
        placeholderToChinese.put("fire", "燃烧几率");
        placeholderToChinese.put("fire_damage", "燃烧伤害");
        placeholderToChinese.put("frozen", "冰冻几率");
        placeholderToChinese.put("frozen_intensity", "冰冻强度");
        placeholderToChinese.put("lightning", "雷击几率");
        placeholderToChinese.put("lightning_damage", "雷击伤害");
        placeholderToChinese.put("hit", "命中几率");
        placeholderToChinese.put("sunder_armor", "破甲几率");
        placeholderToChinese.put("see_through", "护甲穿透");
        placeholderToChinese.put("break_shield", "破盾几率");
        placeholderToChinese.put("armor", "护甲值");
        placeholderToChinese.put("crit_resist", "暴击抵抗");
        placeholderToChinese.put("vampire_resist", "吸血抵抗");
        placeholderToChinese.put("reflection", "反弹几率");
        placeholderToChinese.put("reflection_rate", "反弹倍率");
        placeholderToChinese.put("dodge", "闪避几率");
        placeholderToChinese.put("shield_block", "盾牌格挡率");
        placeholderToChinese.put("remote_immune", "箭伤免疫率");
        placeholderToChinese.put("moving", "移速加成");
        placeholderToChinese.put("restore", "生命恢复");
        placeholderToChinese.put("restore_ratio", "百分比恢复");
        
        String chinese = placeholderToChinese.get(apKey);
        return (chinese != null) ? chinese : apKey;
    }

    private String toPascalCase(String key) {
        if (key == null || key.isEmpty()) return key;
        String[] parts = key.split("_");
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            // 保留全大写缩写（如 pve -> PVE）
            if (p.length() <= 3) {
                b.append(p.toUpperCase());
                continue;
            }
            b.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) b.append(p.substring(1));
        }
        return b.toString();
    }

    public void clear(Player player) {
        if (!enabled) return;
        boolean debug = false;
        try { debug = plugin.getConfig().getBoolean("settings.debug", false); } catch (Throwable ignore) {}
        if (debug) plugin.getLogger().info("[AP] 清理 " + player.getName());

        // 优先 API 模式
        if (apiMode) {
            try {
                Class<?> apiClass = Class.forName("org.serverct.ersha.api.AttributeAPI");
                Object attributeData = apiClass.getMethod("getAttrData", org.bukkit.entity.LivingEntity.class).invoke(null, player);
                Class<?> attributeDataClass = Class.forName("org.serverct.ersha.attribute.data.AttributeData");
                apiClass.getMethod("takeSourceAttribute", attributeDataClass, String.class)
                        .invoke(null, attributeData, namespace);
                lastApplied.remove(player.getUniqueId());
                // 清理后请求一次重算
                try {
                    Class<?> apiClass2 = Class.forName("org.serverct.ersha.api.AttributeAPI");
                    apiClass2.getMethod("updateAttribute", org.bukkit.entity.LivingEntity.class).invoke(null, player);
                } catch (Throwable ignore) {}
                return;
            } catch (Throwable apiError) {
                plugin.getLogger().warning("[AP] API 清理失败，回退命令模式: " + apiError.getMessage());
            }
        }

        // 命令模式清理
        if (commandModeEnabled) {
            if (!clearAllTemplates.isEmpty()) {
                for (String tpl : clearAllTemplates) {
                    String cmd = tpl
                            .replace("%player%", player.getName())
                            .replace("%uuid%", player.getUniqueId().toString())
                            .replace("%namespace%", namespace);
                    boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    if (!ok) {
                        plugin.getLogger().warning("[AP] 清理命令执行失败, 请检查 AttributePlus 指令模板是否正确: " + cmd);
                    }
                }
            } else {
                Map<String, Double> applied = lastApplied.getOrDefault(player.getUniqueId(), Collections.emptyMap());
                if (!applied.isEmpty() && !applyEachTemplates.isEmpty()) {
                    for (String key : applied.keySet()) {
                        for (String tpl : applyEachTemplates) {
                            String cmd = tpl
                                    .replace("%player%", player.getName())
                                    .replace("%uuid%", player.getUniqueId().toString())
                                    .replace("%namespace%", namespace)
                                    .replace("%key%", key)
                                    .replace("%value%", "0");
                            boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            if (!ok) {
                                plugin.getLogger().warning("[AP] 清理命令执行失败, 请检查 AttributePlus 指令模板是否正确: " + cmd);
                            }
                        }
                    }
                }
            }
        }
        lastApplied.remove(player.getUniqueId());
        // 命令模式清理后也触发一次重算
        try {
            Class<?> apiClass2 = Class.forName("org.serverct.ersha.api.AttributeAPI");
            apiClass2.getMethod("updateAttribute", org.bukkit.entity.LivingEntity.class).invoke(null, player);
        } catch (Throwable ignore) {}
    }

    private boolean isPercentageKey(String apKey) {
        if (percentageKeys.contains(apKey)) return true;
        // 兜底：若映射源为 *_PCT 则按百分比处理
        for (Map.Entry<RelicStatType, String> e : map.entrySet()) {
            if (Objects.equals(e.getValue(), apKey) && e.getKey().name().endsWith("_PCT")) return true;
        }
        return false;
    }

    private Map<String, Double> translate(Map<RelicStatType, Double> stats) {
        Map<String, Double> m = new HashMap<>();
        stats.forEach((k, v) -> {
            String ap = map.get(k);
            if (ap != null) m.put(ap, v);
        });
        return m;
    }
}


