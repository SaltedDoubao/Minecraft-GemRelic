package com.lymc.gemrelic.relic;

import java.util.*;

/**
 * 玩家圣遗物档案：当前穿戴的5件 + 仓库
 */
public class PlayerRelicProfile {
    private final UUID playerId;
    private final Map<RelicSlot, RelicData> equipped = new EnumMap<>(RelicSlot.class);
    private final List<RelicData> warehouse = new ArrayList<>();

    public PlayerRelicProfile(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() { return playerId; }
    public Map<RelicSlot, RelicData> getEquipped() { return equipped; }
    public List<RelicData> getWarehouse() { return warehouse; }

    public void equip(RelicData relic) {
        if (relic == null) return;
        // 如果原来有装备，放回仓库
        RelicData old = equipped.get(relic.getSlot());
        if (old != null) {
            warehouse.add(old);
        }
        equipped.put(relic.getSlot(), relic);
        // 从仓库移除（如果存在）
        warehouse.remove(relic);
    }

    public void unequip(RelicSlot slot) {
        RelicData old = equipped.remove(slot);
        if (old != null) {
            warehouse.add(old);
        }
    }

    public void addToWarehouse(RelicData relic) {
        if (relic != null) {
            warehouse.add(relic);
        }
    }

    public boolean removeFromWarehouse(RelicData relic) {
        return warehouse.remove(relic);
    }

    /**
     * 获取指定部位的仓库圣遗物（用于筛选）
     */
    public List<RelicData> getWarehouseBySlot(RelicSlot slot) {
        return warehouse.stream()
                .filter(r -> r.getSlot() == slot)
                .sorted((a, b) -> Integer.compare(b.getLevel(), a.getLevel())) // 按等级降序
                .toList();
    }
}


