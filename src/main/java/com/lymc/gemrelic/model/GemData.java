package com.lymc.gemrelic.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * 宝石数据模型类
 * 用于存储从配置文件读取的宝石定义信息
 */
public class GemData {
    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final List<AttributeData> attributes;
    private final UpgradeConfig upgradeConfig;

    /**
     * 构造函数
     *
     * @param id            宝石唯一标识符
     * @param material      物品材质
     * @param displayName   显示名称
     * @param lore          物品描述
     * @param attributes    属性池列表
     * @param upgradeConfig 升级配置
     */
    public GemData(String id, Material material, String displayName, 
                   List<String> lore, List<AttributeData> attributes, 
                   UpgradeConfig upgradeConfig) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
        this.attributes = attributes != null ? new ArrayList<>(attributes) : new ArrayList<>();
        this.upgradeConfig = upgradeConfig;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return new ArrayList<>(lore);
    }

    public List<AttributeData> getAttributes() {
        return new ArrayList<>(attributes);
    }

    public UpgradeConfig getUpgradeConfig() {
        return upgradeConfig;
    }

    @Override
    public String toString() {
        return "GemData{" +
                "id='" + id + '\'' +
                ", material=" + material +
                ", displayName='" + displayName + '\'' +
                ", attributes=" + attributes.size() +
                '}';
    }
}

