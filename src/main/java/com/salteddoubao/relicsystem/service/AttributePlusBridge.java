package com.salteddoubao.relicsystem.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.RelicStatType;

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

    // ✅ 缓存反射对象
    private Class<?> apiClass;
    private Class<?> attributeDataClass;
    private java.lang.reflect.Method getAttrDataMethod;
    private java.lang.reflect.Method addSourceAttributeMethod;
    private java.lang.reflect.Method takeSourceAttributeMethod;
    private java.lang.reflect.Method updateAttributeMethod;

    // ✅ 属性名称映射（占位符 -> 中文服务器键）
    private final Map<String, String> nameMapping = new HashMap<>();

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
        
        // ✅ 加载属性名称映射
        loadNameMapping();

        // ✅ 初始化反射对象
        if (enabled && apiMode) {
            initializeReflection();
        }

        // 启动时输出配置状态
        if (enabled) {
            plugin.getLogger().info("[AP Bridge] AttributePlus 集成已启用 - namespace=" + namespace + ", apiMode=" + apiMode + ", stat_map映射数=" + map.size());
            if (apiClass != null) {
                plugin.getLogger().info("[AP Bridge] 反射对象初始化成功");
            }
        }
    }

    /**
     * 加载属性名称映射
     */
    private void loadNameMapping() {
        org.bukkit.configuration.ConfigurationSection mappingSection = 
            plugin.getConfig().getConfigurationSection("integration.attributeplus.name_mapping");
        
        if (mappingSection != null && !mappingSection.getKeys(false).isEmpty()) {
            // 从配置加载映射
            for (String key : mappingSection.getKeys(false)) {
                nameMapping.put(key, mappingSection.getString(key));
            }
            plugin.getLogger().info("[AP Bridge] 从配置加载了 " + nameMapping.size() + " 个属性名称映射");
        } else {
            // 使用默认映射
            loadDefaultNameMapping();
            plugin.getLogger().info("[AP Bridge] 使用默认属性名称映射");
        }
    }

    /**
     * 加载默认属性名称映射
     */
    private void loadDefaultNameMapping() {
        nameMapping.put("health", "生命力");
        nameMapping.put("attack", "物理伤害");
        nameMapping.put("defense", "物理防御");
        nameMapping.put("pvp_attack", "PVP伤害");
        nameMapping.put("pve_attack", "PVE伤害");
        nameMapping.put("pvp_defense", "PVP防御");
        nameMapping.put("pve_defense", "PVE防御");
        nameMapping.put("real_attack", "真实伤害");
        nameMapping.put("crit", "暴击几率");
        nameMapping.put("crit_rate", "暴伤倍率");
        nameMapping.put("vampire", "吸血几率");
        nameMapping.put("vampire_rate", "吸血倍率");
        nameMapping.put("shoot_speed", "箭矢速度");
        nameMapping.put("shoot_spread", "箭术精准");
        nameMapping.put("shoot_through", "箭矢穿透率");
        nameMapping.put("fire", "燃烧几率");
        nameMapping.put("fire_damage", "燃烧伤害");
        nameMapping.put("frozen", "冰冻几率");
        nameMapping.put("frozen_intensity", "冰冻强度");
        nameMapping.put("lightning", "雷击几率");
        nameMapping.put("lightning_damage", "雷击伤害");
        nameMapping.put("hit", "命中几率");
        nameMapping.put("sunder_armor", "破甲几率");
        nameMapping.put("see_through", "护甲穿透");
        nameMapping.put("break_shield", "破盾几率");
        nameMapping.put("armor", "护甲值");
        nameMapping.put("crit_resist", "暴击抵抗");
        nameMapping.put("vampire_resist", "吸血抵抗");
        nameMapping.put("reflection", "反弹几率");
        nameMapping.put("reflection_rate", "反弹倍率");
        nameMapping.put("dodge", "闪避几率");
        nameMapping.put("shield_block", "盾牌格挡率");
        nameMapping.put("remote_immune", "箭伤免疫率");
        nameMapping.put("moving", "移速加成");
        nameMapping.put("restore", "生命恢复");
        nameMapping.put("restore_ratio", "百分比恢复");
    }

    /**
     * 初始化反射对象
     */
    private void initializeReflection() {
        try {
            apiClass = Class.forName("org.serverct.ersha.api.AttributeAPI");
            attributeDataClass = Class.forName("org.serverct.ersha.attribute.data.AttributeData");
            getAttrDataMethod = apiClass.getMethod("getAttrData", org.bukkit.entity.LivingEntity.class);
            addSourceAttributeMethod = apiClass.getMethod("addSourceAttribute", attributeDataClass, String.class, List.class);
            takeSourceAttributeMethod = apiClass.getMethod("takeSourceAttribute", attributeDataClass, String.class);
            updateAttributeMethod = apiClass.getMethod("updateAttribute", org.bukkit.entity.LivingEntity.class);
        } catch (Exception e) {
            plugin.getLogger().warning("[AP Bridge] 反射对象初始化失败: " + e.getMessage());
            apiClass = null; // 标记初始化失败
        }
    }

    public boolean isEnabled() { return enabled; }

    public boolean apply(Player player, Map<RelicStatType, Double> stats) {
        if (!enabled) return false;
        boolean debug = plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode();

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
            String serverKey = resolveServerKey(apKey);
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
        if (apiMode && apiClass != null) {
            try {
                // ✅ 使用缓存的反射对象
                Object attributeData = getAttrDataMethod.invoke(null, player);
                
                // 使用List<String>格式的addSourceAttribute方法（让AP解析字符串）
                addSourceAttributeMethod.invoke(null, attributeData, namespace, attributeLines);
                
                plugin.getLogger().info("[AP] addSourceAttribute(List) 调用成功");

                // 记录已应用
                HashMap<String, Double> appliedJoin = new HashMap<>();
                for (Map.Entry<RelicStatType, Double> e : stats.entrySet()) {
                    String apKey = map.get(e.getKey());
                    if (apKey != null) {
                        String serverKey = resolveServerKey(apKey);
                        appliedJoin.merge(serverKey, e.getValue(), Double::sum);
                    }
                }
                lastApplied.put(player.getUniqueId(), appliedJoin);
                // 触发 AP 重算，确保 /ap stats 面板即时刷新（延迟1 tick确保属性已写入）
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        // ✅ 使用缓存的反射对象
                        updateAttributeMethod.invoke(null, player);
                        plugin.getLogger().info("[AP] updateAttribute 调用成功 - 玩家: " + player.getName());
                    } catch (Exception e) {
                        if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode()) {
                            plugin.getLogger().warning("[AP] 触发属性重算失败: " + e.getMessage());
                        }
                    }
                }, 1L);
                return !(appliedJoin.isEmpty());
            } catch (Exception apiError) {
                plugin.getLogger().warning("[AP] API 模式调用失败: " + apiError.getMessage());
                if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode() && apiError.getCause() != null) {
                    plugin.getLogger().warning("  原因: " + apiError.getCause().getMessage());
                }
            }
        }

        plugin.getLogger().warning("[AP] API 模式调用失败，属性下发失败");
        return false;
    }

    /**
     * ✅ 解析服务器键（占位符 -> 中文名称）
     * 使用配置的映射表，如果找不到则返回原键
     */
    private String resolveServerKey(String apKey) {
        return nameMapping.getOrDefault(apKey, apKey);
    }


    public void clear(Player player) {
        if (!enabled) return;
        boolean debug = plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode();
        if (debug) plugin.getLogger().info("[AP] 清理 " + player.getName());

        // 优先 API 模式
        if (apiMode && apiClass != null) {
            try {
                // ✅ 使用缓存的反射对象
                Object attributeData = getAttrDataMethod.invoke(null, player);
                takeSourceAttributeMethod.invoke(null, attributeData, namespace);
                lastApplied.remove(player.getUniqueId());
                plugin.getLogger().info("[AP] takeSourceAttribute 调用成功");
                // 清理后请求一次重算（延迟1 tick）
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        // ✅ 使用缓存的反射对象
                        updateAttributeMethod.invoke(null, player);
                        plugin.getLogger().info("[AP] 清理后 updateAttribute 调用成功 - 玩家: " + player.getName());
                    } catch (Exception e) {
                        if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode()) {
                            plugin.getLogger().warning("[AP] 清理后触发属性重算失败: " + e.getMessage());
                        }
                    }
                }, 1L);
                return;
            } catch (Exception apiError) {
                plugin.getLogger().warning("[AP] API 清理失败: " + apiError.getMessage());
                if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugMode() && apiError.getCause() != null) {
                    plugin.getLogger().warning("  原因: " + apiError.getCause().getMessage());
                }
            }
        }

        lastApplied.remove(player.getUniqueId());
    }

}


