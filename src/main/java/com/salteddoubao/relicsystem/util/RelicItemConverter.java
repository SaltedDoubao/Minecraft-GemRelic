package com.salteddoubao.relicsystem.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.*;

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
            material = getRarityMaterial(relic.getRarity());
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // 设置显示名称
            RelicSet setRef = set;
            if (setRef == null) setRef = plugin.getRelicManager().getRelicSet(relic.getSetId());
            String setName = setRef != null ? setRef.getName() : relic.getSetId();
            meta.setDisplayName(getRarityColor(relic.getRarity()) + setName + " - " + getSlotDisplayName(relic.getSlot()));
            
            // 设置描述
            List<String> lore = new ArrayList<>();
            lore.add("§7等级: §f" + relic.getLevel());
            lore.add("§7");
            
            // 套装效果预览
            if (setRef != null) {
                lore.add("§6套装效果:");
                lore.add("  §e两件套");
                for (String desc : setRef.getTwoPieceEffects()) {
                    lore.add("    §7- " + desc);
                }
                lore.add("  §e四件套");
                for (String desc : setRef.getFourPieceEffects()) {
                    lore.add("    §7- " + desc);
                }
                lore.add("§7");
            }
            
            lore.add("§6主词条:");
            lore.add("  " + relic.getMainStat().getType().getDisplay() + ": §a" + 
                String.format("%.1f", relic.getMainStat().getValue()) + 
                (relic.getMainStat().getType().isPercent() ? "%" : ""));
            
            if (!relic.getSubstats().isEmpty()) {
                lore.add("§7");
                lore.add("§6副词条:");
                for (RelicSubstat substat : relic.getSubstats()) {
                    lore.add("  " + substat.getType().getDisplay() + ": §a" + 
                        String.format("%.1f", substat.getValue()) + 
                        (substat.getType().isPercent() ? "%" : ""));
                }
            }
            
            lore.add("§7");
            if (relic.isLocked()) {
                lore.add("§c🔒 已锁定");
            }
            lore.add("§7");
            lore.add("§e右键装备 | Shift+右键放入仓库");
            
            meta.setLore(lore);
            
            // 存储圣遗物数据
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(relicDataKey, PersistentDataType.STRING, serializeRelicData(relic));
            
            // 非0级时附魔发光
            if (relic.getLevel() > 0) {
                try {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } catch (Throwable ignore) {}
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
            
            // 主词条
            String[] mainParts = parts[7].split(":");
            RelicMainStat mainStat = new RelicMainStat(
                RelicStatType.valueOf(mainParts[0]),
                Double.parseDouble(mainParts[1])
            );
            
            // 副词条
            List<RelicSubstat> substats = new ArrayList<>();
            if (parts.length > 8 && !parts[8].isEmpty()) {
                String[] subParts = parts[8].split(";");
                for (String subPart : subParts) {
                    if (!subPart.isEmpty()) {
                        String[] subData = subPart.split(":");
                        if (subData.length == 2) {
                            substats.add(new RelicSubstat(
                                RelicStatType.valueOf(subData[0]),
                                Double.parseDouble(subData[1])
                            ));
                        }
                    }
                }
            }
            
            return new RelicData(id, setId, slot, rarity, level, exp, mainStat, substats, locked);
        } catch (Exception e) {
            return null;
        }
    }
    
    private Material getRarityMaterial(RelicRarity rarity) {
        return switch (rarity) {
            case WHITE -> Material.QUARTZ;
            case GREEN -> Material.EMERALD;
            case BLUE -> Material.LAPIS_LAZULI;
            case PURPLE -> Material.AMETHYST_SHARD;
            case GOLD -> Material.GOLD_INGOT;
        };
    }
    
    private String getRarityColor(RelicRarity rarity) {
        return switch (rarity) {
            case WHITE -> "§f";
            case GREEN -> "§a";
            case BLUE -> "§9";
            case PURPLE -> "§d";
            case GOLD -> "§6";
        };
    }
    
    private String getSlotDisplayName(RelicSlot slot) {
        return switch (slot) {
            case FLOWER -> "生之花";
            case PLUME -> "死之羽";
            case SANDS -> "时之沙";
            case GOBLET -> "空之杯";
            case CIRCLET -> "理之冠";
        };
    }
}
