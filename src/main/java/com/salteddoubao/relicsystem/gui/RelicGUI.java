package com.salteddoubao.relicsystem.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.relic.RelicSlot;

import java.util.List;

public class RelicGUI {
    public static final String TITLE = "§6§l圣遗物";

    // 简化：一个54格界面，前5个固定槽代表五个部位
    private static final int SLOT_FLOWER = 10;
    private static final int SLOT_PLUME  = 12;
    private static final int SLOT_SANDS  = 14;
    private static final int SLOT_GOBLET = 28;
    private static final int SLOT_CIRCLET= 34;


    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        inv.setItem(SLOT_FLOWER, hint(Material.BLUE_ORCHID, "§e生之花"));
        inv.setItem(SLOT_PLUME,  hint(Material.FEATHER,     "§e死之羽"));
        inv.setItem(SLOT_SANDS,  hint(Material.CLOCK,       "§e时之沙"));
        inv.setItem(SLOT_GOBLET, hint(Material.POTION,      "§e空之杯"));
        inv.setItem(SLOT_CIRCLET,hint(Material.PLAYER_HEAD, "§e理之冠"));
        player.openInventory(inv);
    }

    private ItemStack hint(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of("§7点击选择/更换此部位圣遗物"));
            it.setItemMeta(meta);
        }
        return it;
    }
}


