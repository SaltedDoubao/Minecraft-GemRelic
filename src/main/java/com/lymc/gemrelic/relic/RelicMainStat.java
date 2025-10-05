package com.lymc.gemrelic.relic;

public class RelicMainStat {
    private final RelicStatType type;
    private double value;

    public RelicMainStat(RelicStatType type, double value) {
        this.type = type;
        this.value = value;
    }

    public RelicStatType getType() { return type; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}


