package com.salteddoubao.relicsystem.relic;

import org.bukkit.Material;

import java.util.List;

public class RelicSet {
    private final String id;
    private final String name;
    private final List<String> twoPieceEffects;  // 先用描述字符串，后续替换为Effect DSL
    private final List<String> fourPieceEffects;
    private final Material templateMaterial; // 用于展示的模板物品（区分套装）

    public RelicSet(String id, String name, List<String> twoPieceEffects, List<String> fourPieceEffects, Material templateMaterial) {
        this.id = id;
        this.name = name;
        this.twoPieceEffects = twoPieceEffects;
        this.fourPieceEffects = fourPieceEffects;
        this.templateMaterial = templateMaterial;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getTwoPieceEffects() { return twoPieceEffects; }
    public List<String> getFourPieceEffects() { return fourPieceEffects; }
    public Material getTemplateMaterial() { return templateMaterial; }
}


