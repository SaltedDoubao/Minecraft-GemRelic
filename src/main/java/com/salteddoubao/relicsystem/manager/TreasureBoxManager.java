package com.salteddoubao.relicsystem.manager;

import java.io.File;
import java.util.*;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.RelicRarity;

public class TreasureBoxManager {
    public static class BoxDef {
        public String id;
        public String displayName;
        public List<String> lore = new ArrayList<>();
        public String material = "CHEST";
        public Integer customModelData = null;
        public boolean glow = false;
        public List<String> sets = new ArrayList<>();
        public List<RelicRarity> rarities = new ArrayList<>();
        public Map<String, Integer> setWeights = new HashMap<>();
        public Map<RelicRarity, Integer> rarityWeights = new HashMap<>();
    }

    private final MinecraftRelicSystem plugin;
    private final Map<String, BoxDef> boxes = new HashMap<>();

    public TreasureBoxManager(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        ensureDefault();
        reload();
    }

    private void ensureDefault() {
        File dir = new File(plugin.getDataFolder(), "relics");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, "treasure_box.yml");
        if (!f.exists()) plugin.saveResource("relics/treasure_box.yml", false);
    }

    public void reload() {
        boxes.clear();
        File f = new File(plugin.getDataFolder(), "relics/treasure_box.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("boxes");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            BoxDef d = new BoxDef();
            d.id = id;
            d.displayName = s.getString("display_name", id);
            d.lore = s.getStringList("lore");
            d.material = s.getString("material", "CHEST");
            if (s.isInt("custom_model_data")) d.customModelData = s.getInt("custom_model_data");
            d.glow = s.getBoolean("glow", false);
            d.sets = s.getStringList("sets");
            for (String r : s.getStringList("rarities")) {
                try { d.rarities.add(RelicRarity.valueOf(r)); } catch (Exception ignore) {}
            }
            ConfigurationSection sw = s.getConfigurationSection("set_weights");
            if (sw != null) for (String k : sw.getKeys(false)) d.setWeights.put(k, Math.max(0, sw.getInt(k, 1)));
            ConfigurationSection rw = s.getConfigurationSection("rarity_weights");
            if (rw != null) for (String k : rw.getKeys(false)) {
                try { d.rarityWeights.put(RelicRarity.valueOf(k), Math.max(0, rw.getInt(k, 1))); } catch (Exception ignore) {}
            }
            boxes.put(id, d);
        }
        plugin.getLogger().info("TreasureBoxManager: 已加载宝箱定义=" + boxes.size());
    }

    public BoxDef get(String id) { return boxes.get(id); }
    public Set<String> ids() { return Collections.unmodifiableSet(boxes.keySet()); }

    public String pickSet(BoxDef d, Random rng) {
        if (d.sets.isEmpty()) return null;
        int total = 0; for (String s : d.sets) total += Math.max(1, d.setWeights.getOrDefault(s, 1));
        int r = rng.nextInt(total) + 1, acc = 0;
        for (String s : d.sets) { acc += Math.max(1, d.setWeights.getOrDefault(s, 1)); if (r <= acc) return s; }
        return d.sets.get(d.sets.size() - 1);
    }

    public RelicRarity pickRarity(BoxDef d, Random rng) {
        if (d.rarities.isEmpty()) return RelicRarity.WHITE;
        int total = 0; for (RelicRarity rr : d.rarities) total += Math.max(1, d.rarityWeights.getOrDefault(rr, 1));
        int r = rng.nextInt(total) + 1, acc = 0;
        for (RelicRarity rr : d.rarities) { acc += Math.max(1, d.rarityWeights.getOrDefault(rr, 1)); if (r <= acc) return rr; }
        return d.rarities.get(d.rarities.size() - 1);
    }
}


