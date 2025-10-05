package com.lymc.gemrelic.model;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * 宝石镶嵌位置枚举
 * 定义宝石可以镶嵌在哪些装备位置上
 */
public enum SocketPosition {
    HELMET("helmet", "头盔", Arrays.asList(
            Material.LEATHER_HELMET,
            Material.CHAINMAIL_HELMET,
            Material.IRON_HELMET,
            Material.GOLDEN_HELMET,
            Material.DIAMOND_HELMET,
            Material.NETHERITE_HELMET,
            Material.TURTLE_HELMET
    )),
    CHESTPLATE("chestplate", "胸甲", Arrays.asList(
            Material.LEATHER_CHESTPLATE,
            Material.CHAINMAIL_CHESTPLATE,
            Material.IRON_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE,
            Material.DIAMOND_CHESTPLATE,
            Material.NETHERITE_CHESTPLATE,
            Material.ELYTRA
    )),
    LEGGINGS("leggings", "护腿", Arrays.asList(
            Material.LEATHER_LEGGINGS,
            Material.CHAINMAIL_LEGGINGS,
            Material.IRON_LEGGINGS,
            Material.GOLDEN_LEGGINGS,
            Material.DIAMOND_LEGGINGS,
            Material.NETHERITE_LEGGINGS
    )),
    BOOTS("boots", "靴子", Arrays.asList(
            Material.LEATHER_BOOTS,
            Material.CHAINMAIL_BOOTS,
            Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS,
            Material.DIAMOND_BOOTS,
            Material.NETHERITE_BOOTS
    )),
    WEAPON("weapon", "武器", Arrays.asList(
            // 剑类
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD,
            // 斧类
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE,
            // 镐类
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE,
            // 铲类
            Material.WOODEN_SHOVEL,
            Material.STONE_SHOVEL,
            Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL,
            Material.NETHERITE_SHOVEL,
            // 锄类
            Material.WOODEN_HOE,
            Material.STONE_HOE,
            Material.IRON_HOE,
            Material.GOLDEN_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE,
            // 远程武器
            Material.BOW,
            Material.CROSSBOW,
            Material.TRIDENT
    ));

    private final String id;
    private final String displayName;
    private final List<Material> validMaterials;

    SocketPosition(String id, String displayName, List<Material> validMaterials) {
        this.id = id;
        this.displayName = displayName;
        this.validMaterials = validMaterials;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Material> getValidMaterials() {
        return validMaterials;
    }

    /**
     * 检查指定材质是否可以镶嵌在此位置
     * @param material 装备材质
     * @return 是否可以镶嵌
     */
    public boolean canSocketOn(Material material) {
        return validMaterials.contains(material);
    }

    /**
     * 根据装备材质获取对应的镶嵌位置
     * @param material 装备材质
     * @return 镶嵌位置，如果不是有效装备则返回null
     */
    public static SocketPosition getPositionByMaterial(Material material) {
        for (SocketPosition position : values()) {
            if (position.canSocketOn(material)) {
                return position;
            }
        }
        return null;
    }

    /**
     * 根据ID获取镶嵌位置
     * @param id 位置ID
     * @return 镶嵌位置，如果ID无效则返回null
     */
    public static SocketPosition getById(String id) {
        for (SocketPosition position : values()) {
            if (position.id.equals(id)) {
                return position;
            }
        }
        return null;
    }
}
