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
    private final Map<UUID, Map<String, Double>> lastApplied = new HashMap<>();

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
        
        // 启动时输出配置状态
        if (enabled) {
            plugin.getLogger().info("[AP Bridge] AttributePlus 集成已启用 - namespace=" + namespace + ", apiMode=" + apiMode + ", stat_map映射数=" + map.size());
        }
    }

    public boolean isEnabled() { return enabled; }

    public boolean apply(Player player, Map<RelicStatType, Double> stats) {
        if (!enabled) return false;
        boolean debug = false;
        try {
            debug = plugin.getConfig().getBoolean("settings.debug", false);
        } catch (Exception e) {
            plugin.getLogger().fine("读取调试配置失败: " + e.getMessage());
        }

        // 生成AP格式的属性字符串列表（像Lore一样的格式）
        List<String> attributeLines = new ArrayList<>();

        // 优先解析 API 类以转换占位键为服务器键（中文）
        for (Map.Entry<RelicStatType, Double> entry : stats.entrySet()) {
            RelicStatType type = entry.getKey();
            double raw = entry.getValue();
            if (raw == 0) continue;
            String apKey = map.get(type);
            if (apKey == null || apKey.isEmpty()) {
                if (debug) plugin.getLogger().warning("[AP] 属性 " + type + " 没有stat_map映射，跳过");
                continue;
            }
            
            // 必须使用中文服务器键（AP 的 attributeNameList 使用中文键索引）
            String serverKey = resolveServerKey(null, apKey);
            // 移除颜色代码（AP不识别颜色代码）
            String cleanKey = serverKey.replaceAll("§[0-9a-fk-or]", "");
            
            boolean pctByType = false;
            try {
                pctByType = type.name().endsWith("_PCT");
            } catch (Exception e) {
                plugin.getLogger().fine("判断属性类型失败: " + e.getMessage());
            }
            boolean pctByKey = (percentageKeys != null && percentageKeys.contains(apKey));
            boolean asPercent = pctByType || pctByKey;

            // 生成AP格式的属性行
            String attributeLine;
            if (asPercent) {
                // 百分比格式：属性名: 数值 (%)
                double scaledValue = raw * percentScale;
                attributeLine = cleanKey + ": " + String.format("%.1f", scaledValue) + " (%)";
            } else {
                // 固定值格式：属性名: 数值
                attributeLine = cleanKey + ": " + String.format("%.1f", raw);
            }
            
            attributeLines.add(attributeLine);
            
            if (debug) {
                plugin.getLogger().info("[AP Debug] 生成属性行: " + type + " -> \"" + attributeLine + "\" (asPercent=" + asPercent + ")");
            }
        }

        // 若无任何可下发属性
        if (attributeLines.isEmpty()) {
            if (debug) plugin.getLogger().info("[AP] 无可下发属性，跳过并视为成功");
            return true;
        }

        // 输出详细的属性列表
        plugin.getLogger().info("[AP] 准备下发 " + player.getName() + " - percentScale=" + percentScale + ", 属性数量=" + attributeLines.size());
        plugin.getLogger().info("[AP] 属性字符串列表:");
        for (String line : attributeLines) {
            plugin.getLogger().info("  \"" + line + "\"");
        }

        // 优先 API 模式 - 使用List<String>格式（让AP像读取Lore一样解析）
        if (apiMode) {
            try {
                Class<?> apiClass = Class.forName("org.serverct.ersha.api.AttributeAPI");
                Object attributeData = apiClass.getMethod("getAttrData", org.bukkit.entity.LivingEntity.class).invoke(null, player);

                Class<?> attributeDataClass = Class.forName("org.serverct.ersha.attribute.data.AttributeData");
                
                // 使用List<String>格式的addSourceAttribute方法（让AP解析字符串）
                apiClass.getMethod("addSourceAttribute", attributeDataClass, String.class, List.class)
                        .invoke(null, attributeData, namespace, attributeLines);
                
                plugin.getLogger().info("[AP] addSourceAttribute(List) 调用成功");

                // 记录已应用
                HashMap<String, Double> appliedJoin = new HashMap<>();
                for (Map.Entry<RelicStatType, Double> e : stats.entrySet()) {
                    String apKey = map.get(e.getKey());
                    if (apKey != null) {
                        String serverKey = resolveServerKey(null, apKey);
                        appliedJoin.merge(serverKey, e.getValue(), Double::sum);
                    }
                }
                lastApplied.put(player.getUniqueId(), appliedJoin);
                // 触发 AP 重算，确保 /ap stats 面板即时刷新（延迟1 tick确保属性已写入）
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        Class<?> apiClass2 = Class.forName("org.serverct.ersha.api.AttributeAPI");
                        apiClass2.getMethod("updateAttribute", org.bukkit.entity.LivingEntity.class).invoke(null, player);
                        plugin.getLogger().info("[AP] updateAttribute 调用成功 - 玩家: " + player.getName());
                    } catch (Exception e) {
                        if (plugin.getConfig().getBoolean("settings.debug", false)) {
                            plugin.getLogger().warning("[AP] 触发属性重算失败: " + e.getMessage());
                        }
                    }
                }, 1L);
                return !(appliedJoin.isEmpty());
            } catch (Exception apiError) {
                plugin.getLogger().warning("[AP] API 模式调用失败，回退命令模式: " + apiError.getMessage());
                if (plugin.getConfig().getBoolean("settings.debug", false) && apiError.getCause() != null) {
                    plugin.getLogger().warning("  原因: " + apiError.getCause().getMessage());
                }
            }
        }

        plugin.getLogger().warning("[AP] API 模式调用失败，属性下发失败");
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
        placeholderToChinese.put("crit_rate", "暴伤倍率");  // 注意：AP中是"暴伤倍率"不是"暴击倍率"
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


    public void clear(Player player) {
        if (!enabled) return;
        boolean debug = false;
        try {
            debug = plugin.getConfig().getBoolean("settings.debug", false);
        } catch (Exception e) {
            // 配置读取失败，使用默认值false
            plugin.getLogger().fine("读取调试配置失败: " + e.getMessage());
        }
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
                plugin.getLogger().info("[AP] takeSourceAttribute 调用成功");
                // 清理后请求一次重算（延迟1 tick）
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        Class<?> apiClass2 = Class.forName("org.serverct.ersha.api.AttributeAPI");
                        apiClass2.getMethod("updateAttribute", org.bukkit.entity.LivingEntity.class).invoke(null, player);
                        plugin.getLogger().info("[AP] 清理后 updateAttribute 调用成功 - 玩家: " + player.getName());
                    } catch (Exception e) {
                        if (plugin.getConfig().getBoolean("settings.debug", false)) {
                            plugin.getLogger().warning("[AP] 清理后触发属性重算失败: " + e.getMessage());
                        }
                    }
                }, 1L);
                return;
            } catch (Exception apiError) {
                plugin.getLogger().warning("[AP] API 清理失败，回退命令模式: " + apiError.getMessage());
                if (plugin.getConfig().getBoolean("settings.debug", false) && apiError.getCause() != null) {
                    plugin.getLogger().warning("  原因: " + apiError.getCause().getMessage());
                }
            }
        }

        lastApplied.remove(player.getUniqueId());
    }

}


