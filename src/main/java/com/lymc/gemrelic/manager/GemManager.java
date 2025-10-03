package com.lymc.gemrelic.manager;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.model.AttributeData;
import com.lymc.gemrelic.model.GemData;
import com.lymc.gemrelic.model.GemInstance;
import com.lymc.gemrelic.model.UpgradeConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * 宝石管理器
 * 负责管理宝石的定义、生成、读取和升级等核心功能
 */
public class GemManager {
    private final GemRelicPlugin plugin;
    private final Map<String, GemData> gemRegistry;
    
    // PersistentDataContainer 键定义
    private NamespacedKey gemTypeKey;
    private NamespacedKey gemLevelKey;
    private NamespacedKey gemAttributesKey;

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     */
    public GemManager(GemRelicPlugin plugin) {
        this.plugin = plugin;
        this.gemRegistry = new HashMap<>();
        initializeKeys();
        loadGems();
    }

    /**
     * 初始化 PersistentDataContainer 键
     */
    private void initializeKeys() {
        gemTypeKey = new NamespacedKey(plugin, "gem_type");
        gemLevelKey = new NamespacedKey(plugin, "gem_level");
        gemAttributesKey = new NamespacedKey(plugin, "gem_attributes");
    }

    /**
     * 从配置文件加载宝石定义
     */
    public void loadGems() {
        gemRegistry.clear();
        
        File gemsFile = new File(plugin.getDataFolder(), "gems.yml");
        if (!gemsFile.exists()) {
            plugin.saveResource("gems.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(gemsFile);
        ConfigurationSection gemsSection = config.getConfigurationSection("gems");

        if (gemsSection == null) {
            plugin.getLogger().warning("配置文件 gems.yml 中未找到 'gems' 配置节！");
            return;
        }

        for (String gemId : gemsSection.getKeys(false)) {
            try {
                GemData gemData = loadGemData(gemId, gemsSection.getConfigurationSection(gemId));
                gemRegistry.put(gemId, gemData);
                plugin.getLogger().info("成功加载宝石: " + gemId);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "加载宝石 " + gemId + " 时出错", e);
            }
        }

        plugin.getLogger().info("共加载了 " + gemRegistry.size() + " 种宝石");
    }

    /**
     * 从配置节加载单个宝石数据
     *
     * @param gemId   宝石ID
     * @param section 配置节
     * @return 宝石数据对象
     */
    private GemData loadGemData(String gemId, ConfigurationSection section) {
        Material material = Material.valueOf(section.getString("material", "DIAMOND"));
        String displayName = section.getString("display", gemId);
        List<String> lore = section.getStringList("lore");

        // 加载属性列表
        List<AttributeData> attributes = new ArrayList<>();
        List<Map<?, ?>> attributesList = section.getMapList("attributes");
        for (Map<?, ?> attrMap : attributesList) {
            String type = (String) attrMap.get("type");
            String name = (String) attrMap.get("name");
            double min = getDouble(attrMap, "min", 1.0);
            double max = getDouble(attrMap, "max", 10.0);
            attributes.add(new AttributeData(type, name, min, max));
        }

        // 加载升级配置
        UpgradeConfig upgradeConfig = null;
        if (section.contains("upgrade")) {
            ConfigurationSection upgradeSection = section.getConfigurationSection("upgrade");
            if (upgradeSection != null) {
                Material costMaterial = Material.valueOf(upgradeSection.getString("cost_material", "DIAMOND"));
                int costAmount = upgradeSection.getInt("cost_amount", 1);
                double successChance = upgradeSection.getDouble("success_chance", 0.8);
                double growthRate = upgradeSection.getDouble("growth_rate", 1.2);
                upgradeConfig = new UpgradeConfig(costMaterial, costAmount, successChance, growthRate);
            }
        }

        return new GemData(gemId, material, displayName, lore, attributes, upgradeConfig);
    }

    /**
     * 安全获取 double 值
     */
    private double getDouble(Map<?, ?> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * 获取宝石定义
     *
     * @param gemType 宝石类型ID
     * @return 宝石数据，不存在则返回 null
     */
    public GemData getGemData(String gemType) {
        return gemRegistry.get(gemType);
    }

    /**
     * 获取所有已注册的宝石类型
     *
     * @return 宝石类型ID集合
     */
    public Set<String> getGemTypes() {
        return Collections.unmodifiableSet(gemRegistry.keySet());
    }

    /**
     * 创建宝石物品
     *
     * @param gemType 宝石类型
     * @param level   宝石等级
     * @return 宝石物品，如果宝石类型不存在则返回 null
     */
    public ItemStack createGem(String gemType, int level) {
        GemData gemData = gemRegistry.get(gemType);
        if (gemData == null) {
            return null;
        }

        // 生成随机属性
        Map<String, Double> attributes = new HashMap<>();
        for (AttributeData attrData : gemData.getAttributes()) {
            attributes.put(attrData.getType(), attrData.generateRandomValue());
        }

        GemInstance gemInstance = new GemInstance(gemType, level, attributes);
        return createGemItemStack(gemData, gemInstance);
    }

    /**
     * 根据宝石数据和实例创建物品
     *
     * @param gemData     宝石定义数据
     * @param gemInstance 宝石实例
     * @return 宝石物品
     */
    private ItemStack createGemItemStack(GemData gemData, GemInstance gemInstance) {
        ItemStack item = new ItemStack(gemData.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // 设置显示名称
        meta.setDisplayName(gemData.getDisplayName() + " §7[Lv." + gemInstance.getLevel() + "]");

        // 设置 Lore（描述）
        List<String> lore = new ArrayList<>(gemData.getLore());
        lore.add("§7");
        lore.add("§6属性:");
        for (Map.Entry<String, Double> entry : gemInstance.getAttributes().entrySet()) {
            String attrName = getAttributeName(gemData, entry.getKey());
            lore.add(String.format("  §e%s: §a+%.1f", attrName, entry.getValue()));
        }
        lore.add("§7");
        lore.add("§7等级: §f" + gemInstance.getLevel());
        meta.setLore(lore);

        // 存储宝石数据到 PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(gemTypeKey, PersistentDataType.STRING, gemInstance.getGemType());
        pdc.set(gemLevelKey, PersistentDataType.INTEGER, gemInstance.getLevel());
        
        // 将属性序列化为字符串存储
        StringBuilder attrString = new StringBuilder();
        for (Map.Entry<String, Double> entry : gemInstance.getAttributes().entrySet()) {
            if (attrString.length() > 0) {
                attrString.append(";");
            }
            attrString.append(entry.getKey()).append(":").append(entry.getValue());
        }
        pdc.set(gemAttributesKey, PersistentDataType.STRING, attrString.toString());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 获取属性显示名称
     */
    private String getAttributeName(GemData gemData, String attributeType) {
        for (AttributeData attr : gemData.getAttributes()) {
            if (attr.getType().equals(attributeType)) {
                return attr.getName();
            }
        }
        return attributeType;
    }

    /**
     * 检查物品是否为宝石
     *
     * @param item 物品
     * @return 是否为宝石
     */
    public boolean isGem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(gemTypeKey, PersistentDataType.STRING);
    }

    /**
     * 从物品读取宝石实例
     *
     * @param item 物品
     * @return 宝石实例，如果不是宝石则返回 null
     */
    public GemInstance getGemInstance(ItemStack item) {
        if (!isGem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String gemType = pdc.get(gemTypeKey, PersistentDataType.STRING);
        Integer level = pdc.get(gemLevelKey, PersistentDataType.INTEGER);
        String attrString = pdc.get(gemAttributesKey, PersistentDataType.STRING);

        if (gemType == null || level == null || attrString == null) {
            return null;
        }

        // 解析属性字符串
        Map<String, Double> attributes = new HashMap<>();
        if (!attrString.isEmpty()) {
            for (String attrPair : attrString.split(";")) {
                String[] parts = attrPair.split(":");
                if (parts.length == 2) {
                    try {
                        attributes.put(parts[0], Double.parseDouble(parts[1]));
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("无法解析属性: " + attrPair);
                    }
                }
            }
        }

        return new GemInstance(gemType, level, attributes);
    }

    /**
     * 重载配置
     */
    public void reload() {
        loadGems();
    }
}

