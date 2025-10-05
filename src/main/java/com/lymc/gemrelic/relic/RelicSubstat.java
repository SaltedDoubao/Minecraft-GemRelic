package com.lymc.gemrelic.relic;

public class RelicSubstat {
    private final RelicStatType type;
    private double value;

    public RelicSubstat(RelicStatType type, double value) {
        this.type = type;
        this.value = value;
    }

    public RelicStatType getType() { return type; }
    public double getValue() { return value; }
    public void add(double delta) { this.value += delta; }
}


