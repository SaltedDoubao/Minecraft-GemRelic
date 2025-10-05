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
    private final List<SocketPosition> socketPositions;

    /**
     * 构造函数
     *
     * @param id            宝石唯一标识符
     * @param material      物品材质
     * @param displayName   显示名称
     * @param lore          物品描述
     * @param attributes    属性池列表
     * @param upgradeConfig 升级配置
     * @param socketPositions 可镶嵌位置列表
     */
    public GemData(String id, Material material, String displayName, 
                   List<String> lore, List<AttributeData> attributes, 
                   UpgradeConfig upgradeConfig, List<SocketPosition> socketPositions) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
        this.attributes = attributes != null ? new ArrayList<>(attributes) : new ArrayList<>();
        this.upgradeConfig = upgradeConfig;
        this.socketPositions = socketPositions != null ? new ArrayList<>(socketPositions) : new ArrayList<>();
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

    public List<SocketPosition> getSocketPositions() {
        return new ArrayList<>(socketPositions);
    }

    /**
     * 检查宝石是否可以镶嵌在指定位置
     * @param position 镶嵌位置
     * @return 是否可以镶嵌
     */
    public boolean canSocketAt(SocketPosition position) {
        return socketPositions.isEmpty() || socketPositions.contains(position);
    }

    /**
     * 检查宝石是否可以镶嵌在指定材质的装备上
     * @param material 装备材质
     * @return 是否可以镶嵌
     */
    public boolean canSocketOn(Material material) {
        SocketPosition position = SocketPosition.getPositionByMaterial(material);
        return position != null && canSocketAt(position);
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

