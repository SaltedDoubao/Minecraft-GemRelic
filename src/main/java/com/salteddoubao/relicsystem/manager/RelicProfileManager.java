package com.salteddoubao.relicsystem.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.*;
import com.salteddoubao.relicsystem.storage.IRelicProfileManager;
import com.salteddoubao.relicsystem.util.RelicIO;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家圣遗物档案管理（M2：内存+YAML持久化）
 */
public class RelicProfileManager implements IRelicProfileManager {
    private final MinecraftRelicSystem plugin;
    private final Map<UUID, PlayerRelicProfile> cache = new ConcurrentHashMap<>();

    public RelicProfileManager(MinecraftRelicSystem plugin) {
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
            List<?> rawList = cfg.getList("warehouse");
            if (rawList != null) {
                for (Object obj : rawList) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) obj;
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
        }
    }

    public void save(Player player) {
        PlayerRelicProfile profile = cache.get(player.getUniqueId());
        if (profile == null) {
            plugin.getLogger().warning("尝试保存不存在的玩家档案: " + player.getName());
            return;
        }
        
        File f = getFile(player.getUniqueId());
        FileConfiguration cfg = new YamlConfiguration();
        
        try {
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
            
            // 保存文件
            cfg.save(f);
            plugin.getLogger().info("已保存玩家圣遗物档案: " + player.getName() + 
                " (装备:" + profile.getEquipped().size() + "件, 仓库:" + profile.getWarehouse().size() + "件)");
        } catch (Exception e) {
            plugin.getLogger().severe("保存玩家圣遗物档案失败: " + player.getName());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存所有在线玩家的数据（用于插件禁用时）
     */
    public void saveAll() {
        plugin.getLogger().info("正在保存所有玩家的圣遗物档案...");
        int count = 0;
        for (UUID uuid : cache.keySet()) {
            org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                save(p);
                count++;
            }
        }
        plugin.getLogger().info("已保存 " + count + " 个玩家的圣遗物档案");
    }
}


