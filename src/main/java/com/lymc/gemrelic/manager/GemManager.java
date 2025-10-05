package com.lymc.gemrelic.manager;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.model.AttributeData;
import com.lymc.gemrelic.model.GemData;
import com.lymc.gemrelic.model.GemInstance;
import com.lymc.gemrelic.model.SocketPosition;
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
    private NamespacedKey socketedGemKey;

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
        socketedGemKey = new NamespacedKey(plugin, "socketed_gem");
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

        // 加载镶嵌位置配置
        List<SocketPosition> socketPositions = new ArrayList<>();
        if (section.contains("socket_positions")) {
            List<String> positionList = section.getStringList("socket_positions");
            for (String positionId : positionList) {
                SocketPosition position = SocketPosition.getById(positionId);
                if (position != null) {
                    socketPositions.add(position);
                } else {
                    plugin.getLogger().warning("未知的镶嵌位置: " + positionId + " (宝石: " + gemId + ")");
                }
            }
        }
        // 如果没有配置镶嵌位置，默认允许所有位置
        if (socketPositions.isEmpty()) {
            socketPositions.addAll(Arrays.asList(SocketPosition.values()));
        }

        return new GemData(gemId, material, displayName, lore, attributes, upgradeConfig, socketPositions);
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
     * 检查装备是否已镶嵌宝石
     *
     * @param equipment 装备物品
     * @return 是否已镶嵌宝石
     */
    public boolean hasSocketedGem(ItemStack equipment) {
        if (equipment == null || !equipment.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = equipment.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(socketedGemKey, PersistentDataType.STRING);
    }

    /**
     * 获取装备已镶嵌的宝石信息
     *
     * @param equipment 装备物品
     * @return 宝石信息字符串，格式为 "gemType:level:attributes"
     */
    public String getSocketedGemInfo(ItemStack equipment) {
        if (equipment == null || !equipment.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = equipment.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(socketedGemKey, PersistentDataType.STRING);
    }

    /**
     * 为装备镶嵌宝石
     *
     * @param equipment 装备物品
     * @param gemInstance 宝石实例
     * @return 镶嵌后的装备物品
     */
    public ItemStack socketGemToEquipment(ItemStack equipment, GemInstance gemInstance) {
        if (equipment == null || gemInstance == null) {
            return equipment;
        }

        ItemStack result = equipment.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return equipment;
        }

        // 获取宝石数据
        GemData gemData = getGemData(gemInstance.getGemType());
        if (gemData == null) {
            return equipment;
        }

        // 添加宝石信息到lore
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("§7");
        lore.add("§6§l镶嵌的宝石:");
        lore.add("  §e" + gemData.getDisplayName() + " §7[Lv." + gemInstance.getLevel() + "]");
        
        // 添加宝石属性
        gemInstance.getAttributes().forEach((type, value) -> {
            String attrName = getAttributeName(gemData, type);
            lore.add(String.format("    §7%s: §a+%.1f", attrName, value));
        });
        
        meta.setLore(lore);

        // 存储镶嵌信息到PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        StringBuilder gemInfo = new StringBuilder();
        gemInfo.append(gemInstance.getGemType()).append(":")
               .append(gemInstance.getLevel()).append(":");
        
        boolean first = true;
        for (Map.Entry<String, Double> entry : gemInstance.getAttributes().entrySet()) {
            if (!first) {
                gemInfo.append(";");
            }
            gemInfo.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        
        pdc.set(socketedGemKey, PersistentDataType.STRING, gemInfo.toString());
        result.setItemMeta(meta);
        
        return result;
    }

    /**
     * 从装备中拆卸宝石
     *
     * @param equipment 装备物品
     * @return 拆卸的宝石物品，如果没有宝石则返回null
     */
    public ItemStack unsocketGemFromEquipment(ItemStack equipment) {
        if (!hasSocketedGem(equipment)) {
            return null;
        }

        String gemInfo = getSocketedGemInfo(equipment);
        if (gemInfo == null) {
            return null;
        }

        // 解析宝石信息
        // 解析格式: gemType:level:attrKey1:val1;attrKey2:val2
        String[] parts = gemInfo.split(":", 3);
        if (parts.length < 2) {
            return null;
        }

        String gemType = parts[0];
        int level;
        try {
            level = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        // 解析属性
        Map<String, Double> attributes = new HashMap<>();
        if (parts.length == 3 && parts[2] != null && !parts[2].isEmpty()) {
            for (String attrPair : parts[2].split(";")) {
                String[] attrParts = attrPair.split(":", 2);
                if (attrParts.length == 2) {
                    try {
                        attributes.put(attrParts[0], Double.parseDouble(attrParts[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // 创建宝石实例
        GemInstance gemInstance = new GemInstance(gemType, level, attributes);
        GemData gemData = getGemData(gemType);
        if (gemData == null) {
            return null;
        }

        return createGemItemStack(gemData, gemInstance);
    }

    /**
     * 从装备中移除宝石信息（不返回宝石）
     *
     * @param equipment 装备物品
     * @return 移除宝石信息后的装备
     */
    public ItemStack removeSocketedGemInfo(ItemStack equipment) {
        if (!hasSocketedGem(equipment)) {
            return equipment;
        }

        ItemStack result = equipment.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return equipment;
        }

        // 移除PDC中的宝石信息
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(socketedGemKey);

        // 移除lore中的宝石信息 - 优化性能
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            List<String> newLore = new ArrayList<>(lore.size()); // 预分配容量
            
            // 使用标记来优化字符串比较
            final String gemSectionStart = "§6§l镶嵌的宝石:";
            final String emptyLine = "§7";
            
            boolean inGemSection = false;
            
            for (String line : lore) {
                if (gemSectionStart.equals(line)) {
                    inGemSection = true;
                    continue;
                }
                
                if (inGemSection) {
                    // 优化：检查是否是其他章节的开始
                    if (line.startsWith("§6§l") && !gemSectionStart.equals(line)) {
                        inGemSection = false;
                        newLore.add(line);
                    }
                    // 跳过宝石相关的行（包括属性行）
                } else {
                    newLore.add(line);
                }
            }
            
            // 移除尾部连续的空行 - 优化后向遍历
            int lastIndex = newLore.size() - 1;
            while (lastIndex >= 0 && emptyLine.equals(newLore.get(lastIndex))) {
                newLore.remove(lastIndex--);
            }
            
            meta.setLore(newLore);
        }

        result.setItemMeta(meta);
        return result;
    }

    /**
     * 重载配置
     */
    public void reload() {
        loadGems();
    }
}

