package com.salteddoubao.relicsystem.relic;

import java.util.List;

public class RelicSet {
    private final String id;
    private final String name;
    private final List<String> twoPieceEffects;  // 先用描述字符串，后续替换为Effect DSL
    private final List<String> fourPieceEffects;

    public RelicSet(String id, String name, List<String> twoPieceEffects, List<String> fourPieceEffects) {
        this.id = id;
        this.name = name;
        this.twoPieceEffects = twoPieceEffects;
        this.fourPieceEffects = fourPieceEffects;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getTwoPieceEffects() { return twoPieceEffects; }
    public List<String> getFourPieceEffects() { return fourPieceEffects; }
}


