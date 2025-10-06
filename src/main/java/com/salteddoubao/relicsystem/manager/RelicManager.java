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
    }

    private void ensureDefaultFiles() {
        File dir = new File(plugin.getDataFolder(), "relics");
        if (!dir.exists()) dir.mkdirs();
        // 仅拷贝 sets.yml，其他文件后续里程碑补充
        File sets = new File(dir, "sets.yml");
        if (!sets.exists()) plugin.saveResource("relics/sets.yml", false);
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
            List<String> two = s.getStringList("bonuses.two_piece_desc");
            List<String> four = s.getStringList("bonuses.four_piece_desc");
            RelicSet rs = new RelicSet(id, name, two, four);
            setRegistry.put(id, rs);
        }
        plugin.getLogger().info("RelicManager: 已加载套装数量=" + setRegistry.size());
    }

    public RelicSet getRelicSet(String id) { return setRegistry.get(id); }
    public Set<String> getRelicSetIds() { return Collections.unmodifiableSet(setRegistry.keySet()); }
}


