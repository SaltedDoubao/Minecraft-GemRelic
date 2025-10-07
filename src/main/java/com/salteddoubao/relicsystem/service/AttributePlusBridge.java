package com.salteddoubao.relicsystem.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.RelicStatType;

import java.text.DecimalFormat;
import java.util.*;

/**
 * AttributePlus 兼容桥接（占位，待API对接）。
 * 当前实现：仅在日志中输出计划下发的AP属性。
 */
public class AttributePlusBridge {
    private final MinecraftRelicSystem plugin;
    private final boolean enabled;
    private final String namespace;
    private final Map<RelicStatType, String> map = new HashMap<>();
    // 通过命令模式对接 AP（无需编译期依赖）
    private final boolean commandModeEnabled;
    private final List<String> applyEachTemplates;
    private final List<String> clearAllTemplates;
    private final Map<UUID, Map<String, Double>> lastApplied = new HashMap<>();
    private final DecimalFormat numberFormat = new DecimalFormat("0.####");

    public AttributePlusBridge(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        boolean installed = Bukkit.getPluginManager().getPlugin("AttributePlus") != null;
        this.enabled = plugin.getConfig().getBoolean("integration.attributeplus.enabled", false) && installed;
        this.namespace = plugin.getConfig().getString("integration.attributeplus.namespace", "GemRelic");
        for (RelicStatType t : RelicStatType.values()) {
            String key = plugin.getConfig().getString("integration.attributeplus.stat_map." + t.name());
            if (key != null) map.put(t, key);
        }
        // 命令模式读取
        this.commandModeEnabled = plugin.getConfig().getBoolean("integration.attributeplus.command_mode.enabled", true);
        this.applyEachTemplates = new ArrayList<>(plugin.getConfig().getStringList("integration.attributeplus.command_mode.apply_each"));
        this.clearAllTemplates = new ArrayList<>(plugin.getConfig().getStringList("integration.attributeplus.command_mode.clear_all"));
    }

    public boolean isEnabled() { return enabled; }

    public void apply(Player player, Map<RelicStatType, Double> stats) {
        if (!enabled) return;
        Map<String, Double> translated = translate(stats);
        plugin.getLogger().info("[AP] 下发 " + player.getName() + " => " + translated);
        // 命令模式下发
        if (commandModeEnabled && !applyEachTemplates.isEmpty()) {
            for (Map.Entry<String, Double> e : translated.entrySet()) {
                String key = e.getKey();
                double value = e.getValue();
                for (String tpl : applyEachTemplates) {
                    String cmd = tpl
                            .replace("%player%", player.getName())
                            .replace("%uuid%", player.getUniqueId().toString())
                            .replace("%namespace%", namespace)
                            .replace("%key%", key)
                            .replace("%value%", numberFormat.format(value));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
            lastApplied.put(player.getUniqueId(), new HashMap<>(translated));
            return;
        }
        // TODO: 若后续引入AP官方API依赖，可在此处直接调用API
    }

    public void clear(Player player) {
        if (!enabled) return;
        plugin.getLogger().info("[AP] 清理 " + player.getName());
        // 命令模式清理
        if (commandModeEnabled) {
            // 优先执行 clear_all 模板（按命名空间一次性清空）
            if (!clearAllTemplates.isEmpty()) {
                for (String tpl : clearAllTemplates) {
                    String cmd = tpl
                            .replace("%player%", player.getName())
                            .replace("%uuid%", player.getUniqueId().toString())
                            .replace("%namespace%", namespace);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            } else {
                // 若未配置 clear_all，则回退为对上次下发的每个键执行 value=0 的 applyEach 模板
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
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        }
                    }
                }
            }
        }
        lastApplied.remove(player.getUniqueId());
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


