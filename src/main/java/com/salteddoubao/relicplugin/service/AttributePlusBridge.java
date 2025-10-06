package com.salteddoubao.relicplugin.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.salteddoubao.relicplugin.MinecraftRelicSystem;
import com.salteddoubao.relicplugin.relic.RelicStatType;

import java.util.HashMap;
import java.util.Map;

/**
 * AttributePlus 兼容桥接（占位，待API对接）。
 * 当前实现：仅在日志中输出计划下发的AP属性。
 */
public class AttributePlusBridge {
    private final MinecraftRelicSystem plugin;
    private final boolean enabled;
    private final String namespace;
    private final Map<RelicStatType, String> map = new HashMap<>();

    public AttributePlusBridge(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        boolean installed = Bukkit.getPluginManager().getPlugin("AttributePlus") != null;
        this.enabled = plugin.getConfig().getBoolean("integration.attributeplus.enabled", false) && installed;
        this.namespace = plugin.getConfig().getString("integration.attributeplus.namespace", "GemRelic");
        for (RelicStatType t : RelicStatType.values()) {
            String key = plugin.getConfig().getString("integration.attributeplus.stat_map." + t.name());
            if (key != null) map.put(t, key);
        }
    }

    public boolean isEnabled() { return enabled; }

    public void apply(Player player, Map<RelicStatType, Double> stats) {
        if (!enabled) return;
        plugin.getLogger().info("[AP] 下发 " + player.getName() + " => " + translate(stats));
        // TODO: 使用AP官方API将namespace下的各属性写入并在清理时删除
    }

    public void clear(Player player) {
        if (!enabled) return;
        plugin.getLogger().info("[AP] 清理 " + player.getName());
        // TODO: 使用AP官方API清理namespace修饰
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


