package com.lymc.gemrelic.manager;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.relic.*;
import com.lymc.gemrelic.util.RelicIO;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家圣遗物档案管理（M2：内存+YAML持久化）
 */
public class RelicProfileManager {
    private final GemRelicPlugin plugin;
    private final Map<UUID, PlayerRelicProfile> cache = new ConcurrentHashMap<>();

    public RelicProfileManager(GemRelicPlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerRelicProfile get(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> {
            PlayerRelicProfile p = new PlayerRelicProfile(id);
            load(player, p);
            return p;
        });
    }

    public void clear(Player player) {
        save(player);
        cache.remove(player.getUniqueId());
    }

    private File getFile(UUID id) {
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, id.toString() + ".yml");
    }

    public void load(Player player, PlayerRelicProfile profile) {
        File f = getFile(player.getUniqueId());
        if (!f.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        
        // 加载已装备的圣遗物
        for (RelicSlot slot : RelicSlot.values()) {
            if (cfg.isConfigurationSection("equipped." + slot.name())) {
                RelicData data = RelicIO.deserializeRelic(cfg.getConfigurationSection("equipped." + slot.name()));
                if (data != null) profile.equip(data);
            }
        }
        
        // 加载仓库中的圣遗物
        if (cfg.isList("warehouse")) {
            List<Map<String, Object>> warehouseList = (List<Map<String, Object>>) cfg.getList("warehouse");
            for (Map<String, Object> itemMap : warehouseList) {
                // 转换为ConfigurationSection进行反序列化
                org.bukkit.configuration.MemoryConfiguration temp = new org.bukkit.configuration.MemoryConfiguration();
                for (Map.Entry<String, Object> entry : itemMap.entrySet()) {
                    temp.set(entry.getKey(), entry.getValue());
                }
                RelicData data = RelicIO.deserializeRelic(temp);
                if (data != null) profile.addToWarehouse(data);
            }
        }
    }

    public void save(Player player) {
        PlayerRelicProfile profile = cache.get(player.getUniqueId());
        if (profile == null) return;
        File f = getFile(player.getUniqueId());
        FileConfiguration cfg = new YamlConfiguration();
        
        // 保存已装备的圣遗物
        for (Map.Entry<RelicSlot, RelicData> e : profile.getEquipped().entrySet()) {
            cfg.createSection("equipped." + e.getKey().name(), RelicIO.serializeRelic(e.getValue()));
        }
        
        // 保存仓库中的圣遗物
        List<Map<String, Object>> warehouseList = new ArrayList<>();
        for (RelicData relic : profile.getWarehouse()) {
            warehouseList.add(RelicIO.serializeRelic(relic));
        }
        cfg.set("warehouse", warehouseList);
        
        try { cfg.save(f); } catch (Exception ignored) {}
    }
}


