package com.salteddoubao.relicsystem.service;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.PlayerRelicProfile;
import com.salteddoubao.relicsystem.relic.RelicData;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 套装统计与应用服务（M2：实现两件/四件基础效果的原版属性修饰；后续接入DSL与AP映射）
 */
public class RelicEffectService {
    private final MinecraftRelicSystem plugin;
    // 已应用的修饰记录：player -> (attribute, modifier)
    private final Map<UUID, List<Applied>> applied = new HashMap<>();

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

        // 聚合词条并尝试下发AP
        Map<com.salteddoubao.relicsystem.relic.RelicStatType, Double> statSum = plugin.getStatAggregationService().aggregate(profile);
        plugin.getAttributePlusBridge().apply(player, statSum);
        plugin.getLogger().info("[Relic] " + player.getName() + " 套装统计: " + count + ", 词条合计=" + statSum);
    }

    public void clear(Player player) {
        // 清理AP
        plugin.getAttributePlusBridge().clear(player);
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
}


