package com.salteddoubao.relicsystem.listener;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.RelicStatType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 内置战斗属性计算监听器（无 AP 依赖）：
 * - 暴击率/暴击伤害
 * - 攻击百分比、元素伤害加成
 * - 防御百分比（作为减伤近似处理）
 */
public class CombatListener implements Listener {
    private final MinecraftRelicSystem plugin;

    public CombatListener(MinecraftRelicSystem plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        // 已移除 AP 集成：始终由内置引擎处理
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        // 仅在手持近战武器时应用伤害类加成，避免空手过强
        if (!isMeleeMainhand(player)) return;
        // 从缓存读取聚合词条
        Map<RelicStatType, Double> stats = plugin.getRelicEffectService().getCachedStats(player);
        if (stats.isEmpty()) return;

        double damage = event.getDamage();

        // 攻击力百分比乘区（与原版基础+平添叠乘）
        double atkPct = stats.getOrDefault(RelicStatType.ATK_PCT, 0.0) / 100.0;
        if (atkPct != 0) damage *= (1.0 + atkPct);

        // 元素伤害加成（作为通用额外乘区）
        double elem = stats.getOrDefault(RelicStatType.ELEM_DMG_ANY, 0.0) / 100.0;
        if (elem != 0) damage *= (1.0 + elem);

        // 暴击：按暴击率判定，暴伤系数应用
        double critRate = stats.getOrDefault(RelicStatType.CRIT_RATE, 0.0) / 100.0;
        double critDmg = stats.getOrDefault(RelicStatType.CRIT_DMG, 0.0) / 100.0; // 例如 50% -> 0.5，额外乘区
        if (critRate > 0) {
            double roll = ThreadLocalRandom.current().nextDouble();
            if (roll < Math.max(0, Math.min(1, critRate))) {
                damage *= (1.0 + Math.max(0, critDmg));
            }
        }

        event.setDamage(damage);
    }

    @EventHandler
    public void onAnyDamage(EntityDamageEvent event) {
        // 已移除 AP 集成：始终由内置引擎处理
        if (!(event.getEntity() instanceof Player player)) return;
        Map<RelicStatType, Double> stats = plugin.getRelicEffectService().getCachedStats(player);
        if (stats.isEmpty()) return;

        // 防御百分比作为简化的减伤：最终伤害 *= (1 - DEF_PCT)
        double defPct = stats.getOrDefault(RelicStatType.DEF_PCT, 0.0) / 100.0;
        if (defPct <= 0) return;
        double damage = event.getDamage();
        damage *= Math.max(0.0, 1.0 - Math.min(0.9, defPct));
        event.setDamage(damage);
    }

    private boolean isMeleeMainhand(Player player) {
        org.bukkit.inventory.ItemStack m = player.getInventory().getItemInMainHand();
        if (m == null) return false;
        String n = m.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("TRIDENT");
    }
}


