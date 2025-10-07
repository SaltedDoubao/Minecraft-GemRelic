package com.salteddoubao.relicsystem.service;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.PlayerRelicProfile;
import com.salteddoubao.relicsystem.relic.RelicData;
import com.salteddoubao.relicsystem.relic.RelicStatType;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 套装统计与应用服务（M2：实现两件/四件基础效果的原版属性修饰；后续接入DSL与AP映射）
 */
public class RelicEffectService {
    private final MinecraftRelicSystem plugin;
    // 已应用的修饰记录：player -> (attribute, modifier)
    private final Map<UUID, List<Applied>> applied = new HashMap<>();
    // 聚合词条缓存，供战斗监听读取
    private final Map<UUID, Map<RelicStatType, Double>> cachedStats = new HashMap<>();

    public RelicEffectService(MinecraftRelicSystem plugin) { this.plugin = plugin; }

    public void refresh(Player player, PlayerRelicProfile profile) {
        // 清理旧的修饰
        clear(player);

        Map<String, Integer> count = new HashMap<>();
        for (RelicData r : profile.getEquipped().values()) {
            count.merge(r.getSetId(), 1, Integer::sum);
        }

        // 示例：角斗士的终幕礼 两件/四件
        int gladiator = count.getOrDefault("gladiator", 0);
        if (gladiator >= 2) {
            // 攻击力 +18% → attack_damage *1.18（MULTIPLY_SCALAR_1，值填0.18）
            applyPercentModifier(player, Attribute.GENERIC_ATTACK_DAMAGE, 0.18, "relic:gladiator:2pc");
        }
        if (gladiator >= 4 && isMeleeMainhand(player)) {
            // 近战普通攻击伤害 +35% -> 近似为攻击力 +35%（演示）
            applyPercentModifier(player, Attribute.GENERIC_ATTACK_DAMAGE, 0.35, "relic:gladiator:4pc");
        }

        // 聚合词条
        Map<RelicStatType, Double> statSum = plugin.getStatAggregationService().aggregate(profile);
        cachedStats.put(player.getUniqueId(), statSum);
        // 内置属性应用（不依赖任何外部插件）
        // 生命（最大生命值）：平添与百分比
        Double hpFlat = statSum.get(RelicStatType.HP_FLAT);
        if (hpFlat != null && hpFlat != 0) {
            applyAdditiveModifier(player, Attribute.GENERIC_MAX_HEALTH, hpFlat, "relic:stat:HP_FLAT");
        }
        Double hpPct = statSum.get(RelicStatType.HP_PCT);
        if (hpPct != null && hpPct != 0) {
            applyPercentModifier(player, Attribute.GENERIC_MAX_HEALTH, hpPct / 100.0, "relic:stat:HP_PCT");
        }
        // 若当前生命超过最大生命，进行一次钳制
        try {
            AttributeInstance maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHp != null && player.getHealth() > maxHp.getValue()) {
                player.setHealth(maxHp.getValue());
            }
        } catch (Throwable ignore) {}

        // 攻速（%）
        Double atkSpeed = statSum.get(RelicStatType.ATK_SPEED);
        if (atkSpeed != null && atkSpeed != 0) {
            applyPercentModifier(player, Attribute.GENERIC_ATTACK_SPEED, atkSpeed / 100.0, "relic:stat:ATK_SPEED");
        }
        // 攻击力（平添）：作为原版攻击力的加法修饰，由伤害事件再叠加百分比与暴击
        Double atkFlat = statSum.get(RelicStatType.ATK_FLAT);
        if (atkFlat != null && atkFlat != 0) {
            applyAdditiveModifier(player, Attribute.GENERIC_ATTACK_DAMAGE, atkFlat, "relic:stat:ATK_FLAT");
        }
        // 移速（%）
        Double moveSpeed = statSum.get(RelicStatType.MOVE_SPEED);
        if (moveSpeed != null && moveSpeed != 0) {
            applyPercentModifier(player, Attribute.GENERIC_MOVEMENT_SPEED, moveSpeed / 100.0, "relic:stat:MOVE_SPEED");
        }
        // 幸运（+）
        Double luck = statSum.get(RelicStatType.LUCK);
        if (luck != null && luck != 0) {
            applyAdditiveModifier(player, Attribute.GENERIC_LUCK, luck, "relic:stat:LUCK");
        }
        // 击退抗性（0-1，来自百分数）
        Double kbRes = statSum.get(RelicStatType.KB_RES);
        if (kbRes != null && kbRes != 0) {
            double v = Math.max(0.0, Math.min(1.0, kbRes / 100.0));
            applyAdditiveModifier(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, v, "relic:stat:KB_RES");
        }
        plugin.getLogger().info("[Relic] " + player.getName() + " 套装统计: " + count + ", 词条合计=" + statSum);
    }

    public void clear(Player player) {
        // 清理缓存与原版属性
        cachedStats.remove(player.getUniqueId());
        List<Applied> list = applied.remove(player.getUniqueId());
        if (list == null) return;
        for (Applied a : list) {
            AttributeInstance inst = player.getAttribute(a.attribute);
            if (inst != null && a.modifier != null) {
                try { inst.removeModifier(a.modifier); } catch (Exception ignored) {}
            }
        }
    }

    private void applyPercentModifier(Player player, Attribute attribute, double value, String key) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        UUID uuid = computeUUID(player, attribute, key);
        // 先移除同名/同UUID，确保不会重复叠加
        removeExisting(inst, uuid, key);
        AttributeModifier mod = new AttributeModifier(uuid, key, value, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        inst.addModifier(mod);
        applied.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(new Applied(attribute, mod));
    }

    private void applyAdditiveModifier(Player player, Attribute attribute, double value, String key) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        UUID uuid = computeUUID(player, attribute, key);
        removeExisting(inst, uuid, key);
        AttributeModifier mod = new AttributeModifier(uuid, key, value, AttributeModifier.Operation.ADD_NUMBER);
        inst.addModifier(mod);
        applied.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(new Applied(attribute, mod));
    }

    private UUID computeUUID(Player player, Attribute attribute, String key) {
        String seed = plugin.getName() + ":" + player.getUniqueId() + ":" + attribute.name() + ":" + key;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isMeleeMainhand(Player player) {
        ItemStack m = player.getInventory().getItemInMainHand();
        if (m == null) return false;
        String n = m.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("TRIDENT");
    }

    private void removeExisting(AttributeInstance inst, UUID uuid, String key) {
        // 复制一份集合以避免并发修改
        for (AttributeModifier m : new java.util.HashSet<>(inst.getModifiers())) {
            try {
                boolean sameId = false;
                try { sameId = uuid.equals(m.getUniqueId()); } catch (Throwable ignored) {}
                if (sameId || key.equals(m.getName())) {
                    inst.removeModifier(m);
                }
            } catch (Exception ignored) {}
        }
    }

    private static class Applied {
        final Attribute attribute; final AttributeModifier modifier;
        Applied(Attribute a, AttributeModifier m) { this.attribute = a; this.modifier = m; }
    }

    public Map<RelicStatType, Double> getCachedStats(Player player) {
        Map<RelicStatType, Double> m = cachedStats.get(player.getUniqueId());
        return m != null ? java.util.Collections.unmodifiableMap(m) : java.util.Collections.emptyMap();
    }
}


