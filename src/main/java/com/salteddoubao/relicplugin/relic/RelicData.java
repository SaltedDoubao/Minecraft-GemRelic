package com.salteddoubao.relicplugin.relic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RelicData {
    private final UUID id;
    private final String setId;
    private final RelicSlot slot;
    private final RelicRarity rarity;
    private int level;
    private int exp;
    private final RelicMainStat mainStat;
    private final List<RelicSubstat> substats;
    private boolean locked;

    public RelicData(UUID id, String setId, RelicSlot slot, RelicRarity rarity, int level, int exp, RelicMainStat mainStat, List<RelicSubstat> substats, boolean locked) {
        this.id = id;
        this.setId = setId;
        this.slot = slot;
        this.rarity = rarity;
        this.level = level;
        this.exp = exp;
        this.mainStat = mainStat;
        this.substats = substats != null ? new ArrayList<>(substats) : new ArrayList<>();
        this.locked = locked;
    }

    public UUID getId() { return id; }
    public String getSetId() { return setId; }
    public RelicSlot getSlot() { return slot; }
    public RelicRarity getRarity() { return rarity; }
    public int getLevel() { return level; }
    public int getExp() { return exp; }
    public RelicMainStat getMainStat() { return mainStat; }
    public List<RelicSubstat> getSubstats() { return new ArrayList<>(substats); }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public void setLevel(int level) { this.level = level; }
    public void setExp(int exp) { this.exp = exp; }
}


