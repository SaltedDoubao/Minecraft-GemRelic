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
        // 仅在持有武器时才应用伤害类加成，避免空手过高
        boolean hasWeapon = isMeleeMainhand(player);
        if (gladiator >= 2 && hasWeapon) {
            applyPercentModifier(player, Attribute.GENERIC_ATTACK_DAMAGE, 0.18, "relic:gladiator:2pc");
        }
        if (gladiator >= 4 && hasWeapon) {
            applyPercentModifier(player, Attribute.GENERIC_ATTACK_DAMAGE, 0.35, "relic:gladiator:4pc");
        }

        // 聚合词条
        Map<RelicStatType, Double> statSum = plugin.getStatAggregationService().aggregate(profile);
        cachedStats.put(player.getUniqueId(), statSum);

        // 若启用 AttributePlus 集成，则优先通过桥接下发属性；失败则回退至内置属性引擎
        if (plugin.getAttributePlusBridge() != null && plugin.getAttributePlusBridge().isEnabled()) {
            try {
                boolean applied = plugin.getAttributePlusBridge().apply(player, statSum);
                if (applied) {
                    // AP 已接管，确保立即刷新一次属性供 /ap stats 面板读取
                    try {
                        Class<?> apiClass2 = Class.forName("org.serverct.ersha.api.AttributeAPI");
                        apiClass2.getMethod("updateAttribute", org.bukkit.entity.LivingEntity.class).invoke(null, player);
                    } catch (Exception e) {
                        if (plugin.getConfig().getBoolean("settings.debug", false)) {
                            plugin.getLogger().warning("[AP] 触发属性重算失败: " + e.getMessage());
                        }
                        // 非关键操作，忽略失败
                    }
                    return; // AP 成功接管
                } else {
                    plugin.getLogger().warning("[Relic] AttributePlus 未能成功下发属性，回退到内置属性应用");
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("向 AttributePlus 下发属性时发生异常，回退内置属性: " + t.getMessage());
            }
        }

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
        } catch (Exception e) {
            // 生命值调整失败不影响主要功能
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                plugin.getLogger().warning("调整玩家生命值失败: " + player.getName() + " - " + e.getMessage());
            }
        }

        // 攻击力（平添）：作为原版攻击力的加法修饰，由伤害事件再叠加百分比与暴击
        Double atkFlat = statSum.get(RelicStatType.ATK_FLAT);
        if (atkFlat != null && atkFlat != 0) {
            applyAdditiveModifier(player, Attribute.GENERIC_ATTACK_DAMAGE, atkFlat, "relic:stat:ATK_FLAT");
        }
        // 移速（%）- 若未启用 AP 则回退到原版属性
        Double moveSpeed = statSum.get(RelicStatType.MOVE_SPEED);
        if (moveSpeed != null && moveSpeed != 0) {
            applyPercentModifier(player, Attribute.GENERIC_MOVEMENT_SPEED, moveSpeed / 100.0, "relic:stat:MOVE_SPEED");
        }
        // 调试日志受配置控制
        try {
            boolean debug = plugin.getConfig().getBoolean("settings.debug", false);
            boolean verbose = plugin.getConfig().getBoolean("relic.debug_effects", false);
            if (debug || verbose) {
                plugin.getLogger().info("[Relic] " + player.getName() + " 套装统计: " + count + ", 词条合计=" + statSum);
            }
        } catch (Exception e) {
            // 配置读取失败，使用默认值false
            plugin.getLogger().fine("读取调试配置失败: " + e.getMessage());
        }
    }

    public void clear(Player player) {
        // 清理缓存与原版属性
        cachedStats.remove(player.getUniqueId());
        List<Applied> list = applied.remove(player.getUniqueId());
        if (list == null) return;
        for (Applied a : list) {
            AttributeInstance inst = player.getAttribute(a.attribute);
            if (inst != null && a.modifier != null) {
                try {
                    inst.removeModifier(a.modifier);
                } catch (Exception e) {
                    plugin.getLogger().warning("移除属性修饰失败 [" + a.attribute + "]: " + e.getMessage());
                }
            }
        }
        // 若启用 AP 集成，清理 AP 侧属性交付
        if (plugin.getAttributePlusBridge() != null && plugin.getAttributePlusBridge().isEnabled()) {
            try {
                plugin.getAttributePlusBridge().clear(player);
            } catch (Throwable t) {
                plugin.getLogger().warning("清理 AttributePlus 属性时发生异常: " + t.getMessage());
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
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return false;
        }
        org.bukkit.Material type = item.getType();
        return type.name().endsWith("_SWORD") 
            || type.name().endsWith("_AXE") 
            || type == org.bukkit.Material.TRIDENT;
    }

    private void removeExisting(AttributeInstance inst, UUID uuid, String key) {
        // 复制一份集合以避免并发修改
        for (AttributeModifier m : new java.util.HashSet<>(inst.getModifiers())) {
            try {
                boolean sameId = false;
                try {
                    sameId = uuid.equals(m.getUniqueId());
                } catch (Exception e) {
                    // 某些Bukkit版本可能不支持getUniqueId
                    plugin.getLogger().fine("获取AttributeModifier UUID失败: " + e.getMessage());
                }
                if (sameId || key.equals(m.getName())) {
                    inst.removeModifier(m);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("移除属性修饰失败 [" + key + "]: " + e.getMessage());
            }
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


