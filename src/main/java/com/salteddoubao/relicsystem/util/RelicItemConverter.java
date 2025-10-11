package com.salteddoubao.relicsystem.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.*;
import com.salteddoubao.relicsystem.util.RelicDisplayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 圣遗物与ItemStack转换工具
 */
public class RelicItemConverter {
    private final MinecraftRelicSystem plugin;
    private final NamespacedKey relicDataKey;
    
    public RelicItemConverter(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        this.relicDataKey = new NamespacedKey(plugin, "relic_data");
    }
    
    /**
     * 将圣遗物转换为ItemStack
     */
    public ItemStack toItemStack(RelicData relic) {
        // 优先使用套装模板物品展示，以区分不同套装
        Material material = null;
        RelicSet set = plugin.getRelicManager().getRelicSet(relic.getSetId());
        if (set != null && set.getTemplateMaterial() != null) {
            material = set.getTemplateMaterial();
        }
        if (material == null) {
            material = RelicDisplayUtils.getRarityMaterial(relic.getRarity());
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // 设置显示名称
            RelicSet setRef = set;
            if (setRef == null) setRef = plugin.getRelicManager().getRelicSet(relic.getSetId());
            String setName = setRef != null ? setRef.getName() : relic.getSetId();
            meta.displayName(Component.text(RelicDisplayUtils.getRarityColor(relic.getRarity()) + setName + " - " + RelicDisplayUtils.getSlotDisplayName(relic.getSlot()))
                .decoration(TextDecoration.ITALIC, false));
            
            // 设置描述
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7等级: §f" + relic.getLevel()));
            lore.add(Component.text("§7"));
            
            // 套装效果预览
            if (setRef != null) {
                lore.add(Component.text("§6套装效果:"));
                lore.add(Component.text("  §e两件套"));
                for (String desc : setRef.getTwoPieceEffects()) {
                    lore.add(Component.text("    §7- " + desc));
                }
                lore.add(Component.text("  §e四件套"));
                for (String desc : setRef.getFourPieceEffects()) {
                    lore.add(Component.text("    §7- " + desc));
                }
                lore.add(Component.text("§7"));
            }
            
            lore.add(Component.text("§6主词条:"));
            lore.add(Component.text("  " + relic.getMainStat().getType().getDisplay() + ": §a" + 
                String.format("%.1f", relic.getMainStat().getValue()) + 
                (relic.getMainStat().getType().isPercent() ? "%" : "")));
            
            if (!relic.getSubstats().isEmpty()) {
                lore.add(Component.text("§7"));
                lore.add(Component.text("§6副词条:"));
                for (RelicSubstat substat : relic.getSubstats()) {
                    lore.add(Component.text("  " + substat.getType().getDisplay() + ": §a" + 
                        String.format("%.1f", substat.getValue()) + 
                        (substat.getType().isPercent() ? "%" : "")));
                }
            }
            
            lore.add(Component.text("§7"));
            if (relic.isLocked()) {
                lore.add(Component.text("§c已锁定"));
            }
            lore.add(Component.text("§7"));
            lore.add(Component.text("§e右键装备 | Shift+右键放入仓库"));
            
            meta.lore(lore);
            
            // 存储圣遗物数据
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(relicDataKey, PersistentDataType.STRING, serializeRelicData(relic));
            
            // 非0级时附魔发光
            if (relic.getLevel() > 0) {
                try {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } catch (Exception e) {
                    if (plugin.getConfig().getBoolean("settings.debug", false)) {
                        plugin.getLogger().warning("为圣遗物添加发光效果失败: " + e.getMessage());
                    }
                    // 非关键功能，继续执行
                }
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 从ItemStack转换回圣遗物
     */
    public RelicData fromItemStack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(relicDataKey, PersistentDataType.STRING)) {
            return null;
        }
        
        String data = pdc.get(relicDataKey, PersistentDataType.STRING);
        return deserializeRelicData(data);
    }
    
    /**
     * 检查物品是否为圣遗物
     */
    public boolean isRelicItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        return item.getItemMeta().getPersistentDataContainer()
            .has(relicDataKey, PersistentDataType.STRING);
    }
    
    private String serializeRelicData(RelicData relic) {
        StringBuilder sb = new StringBuilder();
        sb.append(relic.getId().toString()).append("|");
        sb.append(relic.getSetId()).append("|");
        sb.append(relic.getSlot().name()).append("|");
        sb.append(relic.getRarity().name()).append("|");
        sb.append(relic.getLevel()).append("|");
        sb.append(relic.getExp()).append("|");
        sb.append(relic.isLocked()).append("|");
        
        // 主词条
        RelicMainStat main = relic.getMainStat();
        sb.append(main.getType().name()).append(":").append(main.getValue()).append("|");
        
        // 副词条
        for (RelicSubstat sub : relic.getSubstats()) {
            sb.append(sub.getType().name()).append(":").append(sub.getValue()).append(";");
        }
        
        return sb.toString();
    }
    
    private RelicData deserializeRelicData(String data) {
        try {
            String[] parts = data.split("\\|");
            if (parts.length < 8) return null;
            
            UUID id = UUID.fromString(parts[0]);
            String setId = parts[1];
            RelicSlot slot = RelicSlot.valueOf(parts[2]);
            RelicRarity rarity = RelicRarity.valueOf(parts[3]);
            int level = Integer.parseInt(parts[4]);
            int exp = Integer.parseInt(parts[5]);
            boolean locked = Boolean.parseBoolean(parts[6]);
            
            // 主词条（兼容旧枚举名）
            String[] mainParts = parts[7].split(":");
            String mainTypeStr = migrateOldStatType(mainParts[0]);
            if (mainTypeStr == null) return null; // 已删除的属性类型
            RelicMainStat mainStat = new RelicMainStat(
                RelicStatType.valueOf(mainTypeStr),
                Double.parseDouble(mainParts[1])
            );
            
            // 副词条（兼容旧枚举名）
            List<RelicSubstat> substats = new ArrayList<>();
            if (parts.length > 8 && !parts[8].isEmpty()) {
                String[] subParts = parts[8].split(";");
                for (String subPart : subParts) {
                    if (!subPart.isEmpty()) {
                        String[] subData = subPart.split(":");
                        if (subData.length == 2) {
                            String subTypeStr = migrateOldStatType(subData[0]);
                            if (subTypeStr != null) { // 跳过已删除的属性
                                substats.add(new RelicSubstat(
                                    RelicStatType.valueOf(subTypeStr),
                                    Double.parseDouble(subData[1])
                                ));
                            }
                        }
                    }
                }
            }
            
            return new RelicData(id, setId, slot, rarity, level, exp, mainStat, substats, locked);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 迁移旧的属性类型名到新名称（兼容性映射）
     */
    private static String migrateOldStatType(String oldType) {
        if (oldType == null) return oldType;
        // 对应 AP 官方属性重命名
        return switch (oldType) {
            case "CRIT_RATE" -> "CRIT_CHANCE";  // 暴击率 -> 暴击几率
            case "CRIT_DMG" -> "CRIT_RATE";     // 暴击伤害 -> 暴伤倍率
            case "HEAL_BONUS" -> "RESTORE_RATIO"; // 治疗加成 -> 百分比恢复
            case "DEFENSE" -> "DEF_FLAT";       // 防御 -> 物理防御（平添）
            case "PVP_DEFENSE" -> "PVP_DEF";
            case "PVE_DEFENSE" -> "PVE_DEF";
            case "SHIELD_BLOCK_CHANCE" -> "SHIELD_BLOCK";
            // 已删除的属性返回 null，跳过
            case "ELEM_DMG_ANY", "ATK_SPEED", "LUCK", "KB_RES" -> null;
            default -> oldType;
        };
    }
    
}
