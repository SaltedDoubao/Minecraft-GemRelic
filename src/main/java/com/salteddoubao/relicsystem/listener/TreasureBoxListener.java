package com.salteddoubao.relicsystem.listener;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.manager.TreasureBoxManager;
import com.salteddoubao.relicsystem.relic.*;
import com.salteddoubao.relicsystem.util.RelicItemConverter;
import com.salteddoubao.relicsystem.util.TreasureBoxItemFactory;

public class TreasureBoxListener implements Listener {
    private final MinecraftRelicSystem plugin;
    private final Random rng = new Random();

    public TreasureBoxListener(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.CHEST) return;
        TreasureBoxItemFactory factory = new TreasureBoxItemFactory(plugin);
        String id = factory.readBoxId(item);
        if (id == null) return;

        TreasureBoxManager.BoxDef def = plugin.getTreasureBoxManager().get(id);
        if (def == null) return;

        e.setCancelled(true);

        String setId = plugin.getTreasureBoxManager().pickSet(def, rng);
        RelicRarity rarity = plugin.getTreasureBoxManager().pickRarity(def, rng);
        RelicSlot slot = RelicSlot.values()[rng.nextInt(RelicSlot.values().length)];

        RelicData data = plugin.getRelicGenerationService().generate(setId, slot, rarity, 0);
        RelicItemConverter converter = plugin.getRelicItemConverter();
        e.getPlayer().getInventory().addItem(converter.toItemStack(data));

        // 消耗一个宝箱
        ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
        if (hand != null && hand.isSimilar(item)) {
            hand.setAmount(hand.getAmount() - 1);
        }

        e.getPlayer().sendMessage("§a你开启了宝箱，获得圣遗物: " + setId + " - " + slot + " (" + rarity + ")");
    }
}


