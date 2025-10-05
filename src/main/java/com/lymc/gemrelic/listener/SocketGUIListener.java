package com.lymc.gemrelic.listener;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.gui.SocketGUI;
import com.lymc.gemrelic.manager.GemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 镶嵌GUI监听器
 * 处理镶嵌界面的交互事件
 */
public class SocketGUIListener implements Listener {
    private final GemRelicPlugin plugin;
    private final GemManager gemManager;
    private final SocketGUI socketGUI;

    public SocketGUIListener(GemRelicPlugin plugin) {
        this.plugin = plugin;
        this.gemManager = plugin.getGemManager();
        this.socketGUI = new SocketGUI(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        
        // 检查是否为镶嵌界面
        if (!isSocketGUI(event.getInventory())) {
            return;
        }

        int slot = event.getSlot();
        
        // 如果点击的是玩家背包区域，允许正常操作
        if (slot >= 54) {
            return; // 玩家背包区域，不做任何限制
        }
        
        // 处理GUI界面中的不同槽位点击
        if (slot == SocketGUI.getEquipmentSlot()) {
            handleEquipmentSlot(event);
        } else if (slot == SocketGUI.getGemSlot()) {
            handleGemSlot(event);
        } else if (slot == SocketGUI.getResultSlot()) {
            // 结果槽位完全禁用交互
            event.setCancelled(true);
        } else if (slot == SocketGUI.getConfirmSlot()) {
            handleConfirmClick(event);
        } else if (slot == SocketGUI.getCancelSlot()) {
            handleCancelClick(event);
        } else if (slot == SocketGUI.getUnsocketSlot()) {
            handleUnsocketClick(event);
        } else {
            // GUI界面的其他槽位（边框等）不允许操作
            event.setCancelled(true);
        }
        
        // 更新预览
        updateGUIPreview(event.getInventory());
    }

    /**
     * 处理装备槽位点击
     */
    private void handleEquipmentSlot(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // 如果是Shift点击，禁止操作
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        // 如果当前槽位是GUI提示物品，允许覆盖
        if (clicked != null && isGUIItem(clicked)) {
            // 清空槽位以便放入新物品
            event.getInventory().setItem(event.getSlot(), null);
        }

        // 如果玩家手持装备，检查是否为有效装备
        if (cursor != null && !cursor.getType().isAir()) {
            if (!isValidEquipment(cursor)) {
                player.sendMessage("§c只能放入头盔、胸甲、护腿或靴子！");
                event.setCancelled(true);
                return;
            }
        }
        
        // 允许正常的放置和取出操作
    }

    /**
     * 处理宝石槽位点击
     */
    private void handleGemSlot(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // 如果是Shift点击，禁止操作
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        // 如果当前槽位是GUI提示物品，允许覆盖
        if (clicked != null && isGUIItem(clicked)) {
            // 清空槽位以便放入新物品
            event.getInventory().setItem(event.getSlot(), null);
        }

        // 如果玩家手持物品，检查是否为宝石
        if (cursor != null && !cursor.getType().isAir()) {
            if (!gemManager.isGem(cursor)) {
                player.sendMessage("§c只能放入宝石！");
                event.setCancelled(true);
                return;
            }
        }
        
        // 允许正常的放置和取出操作
    }

    /**
     * 处理确认按钮点击
     */
    private void handleConfirmClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        Inventory gui = event.getInventory();
        
        ItemStack equipment = gui.getItem(SocketGUI.getEquipmentSlot());
        ItemStack gem = gui.getItem(SocketGUI.getGemSlot());
        
        if (equipment == null || equipment.getType().isAir()) {
            player.sendMessage("§c请先放入要镶嵌的装备！");
            return;
        }
        
        if (gem == null || gem.getType().isAir()) {
            player.sendMessage("§c请先放入要镶嵌的宝石！");
            return;
        }
        
        // 执行镶嵌操作
        ItemStack result = socketGUI.performSocket(player, equipment, gem);
        if (result != null) {
            // 镶嵌成功，将结果给玩家，重置界面
            player.getInventory().addItem(result);
            
            // 重新设置GUI提示物品
            resetGUISlots(gui);
            
            // 关闭界面
            player.closeInventory();
        }
    }

    /**
     * 处理取消按钮点击
     */
    private void handleCancelClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        Inventory gui = event.getInventory();
        
        // 退还物品
        returnItems(player, gui);
        
        // 关闭界面
        player.closeInventory();
    }

    /**
     * 处理拆卸按钮点击
     */
    private void handleUnsocketClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        Inventory gui = event.getInventory();
        
        ItemStack equipment = gui.getItem(SocketGUI.getEquipmentSlot());
        
        if (equipment == null || equipment.getType().isAir() || isGUIItem(equipment)) {
            player.sendMessage("§c请先放入已镶嵌宝石的装备！");
            return;
        }
        
        // 执行拆卸操作
        ItemStack gem = socketGUI.performUnsocket(player, equipment);
        if (gem != null) {
            // 拆卸成功，给玩家宝石，移除装备上的宝石信息
            player.getInventory().addItem(gem);
            
            // 更新装备（移除宝石信息）
            ItemStack updatedEquipment = gemManager.removeSocketedGemInfo(equipment);
            gui.setItem(SocketGUI.getEquipmentSlot(), updatedEquipment);
            
            // 更新预览
            updateGUIPreview(gui);
        }
    }

    /**
     * 监听界面关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        
        if (!isSocketGUI(event.getInventory())) {
            return;
        }

        // 退还物品
        returnItems(player, event.getInventory());
    }

    /**
     * 退还界面中的物品给玩家
     */
    private void returnItems(Player player, Inventory gui) {
        ItemStack equipment = gui.getItem(SocketGUI.getEquipmentSlot());
        ItemStack gem = gui.getItem(SocketGUI.getGemSlot());
        
        // 只退还真正的物品，不退还GUI提示物品
        if (equipment != null && !equipment.getType().isAir() && !isGUIItem(equipment)) {
            player.getInventory().addItem(equipment);
        }
        
        if (gem != null && !gem.getType().isAir() && !isGUIItem(gem)) {
            player.getInventory().addItem(gem);
        }
    }

    /**
     * 检查物品是否为GUI提示物品
     */
    private boolean isGUIItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        // 检查是否为边框物品
        if (item.getType().name().contains("GLASS_PANE")) {
            return true;
        }
        
        if (!item.hasItemMeta()) {
            return false;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        
        // 检查是否为GUI功能物品
        return "§e放入要镶嵌的装备".equals(displayName) ||
               "§e放入宝石".equals(displayName) ||
               "§c镶嵌结果".equals(displayName) ||
               "§a§l确认镶嵌".equals(displayName) ||
               "§c§l取消操作".equals(displayName) ||
               "§6§l拆卸宝石".equals(displayName) ||
               "§c无法镶嵌".equals(displayName) ||
               "§7".equals(displayName);
    }

    /**
     * 重置GUI槽位为提示物品状态
     */
    private void resetGUISlots(Inventory gui) {
        // 重置装备槽位
        ItemStack equipmentHint = createGuiItem(org.bukkit.Material.ARMOR_STAND, 
            "§e放入要镶嵌的装备", 
            java.util.Arrays.asList("§7将要镶嵌宝石的装备放在这里", "§7支持：头盔、胸甲、护腿、靴子"));
        gui.setItem(SocketGUI.getEquipmentSlot(), equipmentHint);
        
        // 重置宝石槽位
        ItemStack gemHint = createGuiItem(org.bukkit.Material.EMERALD, 
            "§e放入宝石", 
            java.util.Arrays.asList("§7将要镶嵌的宝石放在这里", "§7确保宝石支持目标装备位置"));
        gui.setItem(SocketGUI.getGemSlot(), gemHint);
        
        // 重置结果槽位
        ItemStack resultHint = createGuiItem(org.bukkit.Material.BARRIER, 
            "§c镶嵌结果", 
            java.util.Arrays.asList("§7镶嵌成功后的装备会在这里显示"));
        gui.setItem(SocketGUI.getResultSlot(), resultHint);
    }

    /**
     * 创建GUI物品
     */
    private ItemStack createGuiItem(org.bukkit.Material material, String displayName, java.util.List<String> lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 更新GUI预览
     */
    private void updateGUIPreview(Inventory gui) {
        // 延迟一个游戏刻更新预览，确保物品已经放置
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack equipment = gui.getItem(SocketGUI.getEquipmentSlot());
            ItemStack gem = gui.getItem(SocketGUI.getGemSlot());
            
            // 检查并恢复占位物品
            checkAndRestorePlaceholders(gui, equipment, gem);
            
            // 更新镶嵌预览
            socketGUI.updateSocketPreview(gui, equipment, gem);
        }, 1L);
    }

    /**
     * 检查并恢复占位物品
     */
    private void checkAndRestorePlaceholders(Inventory gui, ItemStack equipment, ItemStack gem) {
        // 如果装备槽位为空或是GUI物品，恢复占位物品
        if (equipment == null || equipment.getType().isAir() || isGUIItem(equipment)) {
            ItemStack equipmentHint = createGuiItem(org.bukkit.Material.ARMOR_STAND, 
                "§e放入要镶嵌的装备", 
                java.util.Arrays.asList("§7将要镶嵌宝石的装备放在这里", "§7支持：头盔、胸甲、护腿、靴子、武器"));
            gui.setItem(SocketGUI.getEquipmentSlot(), equipmentHint);
        }
        
        // 如果宝石槽位为空或是GUI物品，恢复占位物品
        if (gem == null || gem.getType().isAir() || isGUIItem(gem)) {
            ItemStack gemHint = createGuiItem(org.bukkit.Material.EMERALD, 
                "§e放入宝石", 
                java.util.Arrays.asList("§7将要镶嵌的宝石放在这里", "§7确保宝石支持目标装备位置"));
            gui.setItem(SocketGUI.getGemSlot(), gemHint);
        }
    }

    /**
     * 检查是否为镶嵌GUI
     */
    private boolean isSocketGUI(Inventory inventory) {
        if (inventory == null || inventory.getType() != InventoryType.CHEST || inventory.getSize() != 54) {
            return false;
        }
        
        // 通过检查界面中的特定物品来识别镶嵌界面
        ItemStack confirmButton = inventory.getItem(SocketGUI.getConfirmSlot());
        ItemStack cancelButton = inventory.getItem(SocketGUI.getCancelSlot());
        
        if (confirmButton != null && cancelButton != null && confirmButton.hasItemMeta() && cancelButton.hasItemMeta()) {
            String confirmName = confirmButton.getItemMeta().getDisplayName();
            String cancelName = cancelButton.getItemMeta().getDisplayName();
            
            return "§a§l确认镶嵌".equals(confirmName) && "§c§l取消操作".equals(cancelName);
        }
        
        return false;
    }

    /**
     * 检查是否为有效的装备
     */
    private boolean isValidEquipment(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        
        // 使用SocketPosition来判断是否为有效装备
        com.lymc.gemrelic.model.SocketPosition position = com.lymc.gemrelic.model.SocketPosition.getPositionByMaterial(item.getType());
        return position != null;
    }

    /**
     * 获取SocketGUI实例
     */
    public SocketGUI getSocketGUI() {
        return socketGUI;
    }
}
