package com.lymc.gemrelic.gui;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.manager.GemManager;
import com.lymc.gemrelic.model.GemData;
import com.lymc.gemrelic.model.GemInstance;
import com.lymc.gemrelic.model.SocketPosition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 宝石镶嵌GUI界面
 * 使用箱子界面进行宝石镶嵌操作
 */
public class SocketGUI {
    private final GemRelicPlugin plugin;
    private final GemManager gemManager;
    
    // GUI槽位定义
    private static final int EQUIPMENT_SLOT = 13;   // 装备槽位（箱子中央）
    private static final int GEM_SLOT = 15;         // 宝石槽位
    private static final int RESULT_SLOT = 31;      // 结果槽位
    private static final int CONFIRM_SLOT = 29;     // 确认按钮
    private static final int CANCEL_SLOT = 33;      // 取消按钮
    private static final int UNSOCKET_SLOT = 22;    // 拆卸按钮
    
    // 缓存GUI物品，避免重复创建
    private ItemStack cachedEquipmentHint;
    private ItemStack cachedGemHint;
    private ItemStack cachedResultHint;
    private ItemStack cachedConfirmButton;
    private ItemStack cachedCancelButton;
    private ItemStack cachedUnsocketButton;
    private ItemStack cachedBorderItem;
    private ItemStack cachedFailureResult;

    public SocketGUI(GemRelicPlugin plugin) {
        this.plugin = plugin;
        this.gemManager = plugin.getGemManager();
        initializeCachedItems();
    }
    
    /**
     * 初始化缓存的GUI物品
     */
    private void initializeCachedItems() {
        // 缓存边框物品
        cachedBorderItem = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
        
        // 缓存装备提示物品
        cachedEquipmentHint = createGuiItem(Material.ARMOR_STAND, 
            "§e放入要镶嵌的装备", 
            List.of("§7将要镶嵌宝石的装备放在这里", "§7支持：头盔、胸甲、护腿、靴子、武器"));
            
        // 缓存宝石提示物品
        cachedGemHint = createGuiItem(Material.EMERALD, 
            "§e放入宝石", 
            List.of("§7将要镶嵌的宝石放在这里", "§7确保宝石支持目标装备位置"));
            
        // 缓存结果提示物品
        cachedResultHint = createGuiItem(Material.BARRIER, 
            "§c镶嵌结果", 
            List.of("§7镶嵌成功后的装备会在这里显示"));
            
        // 缓存失败结果物品
        cachedFailureResult = createGuiItem(Material.BARRIER, 
            "§c无法镶嵌", 
            List.of("§7这个宝石无法镶嵌在此装备上"));
            
        // 缓存功能按钮
        cachedConfirmButton = createGuiItem(Material.GREEN_CONCRETE, 
            "§a§l确认镶嵌", 
            List.of("§7点击完成镶嵌操作"));
            
        cachedCancelButton = createGuiItem(Material.RED_CONCRETE, 
            "§c§l取消操作", 
            List.of("§7点击取消并退出"));
            
        cachedUnsocketButton = createGuiItem(Material.ORANGE_CONCRETE, 
            "§6§l拆卸宝石", 
            List.of("§7点击拆卸装备上的宝石", "§7需要先放入已镶嵌宝石的装备"));
    }

    /**
     * 为玩家打开镶嵌界面
     * @param player 玩家
     */
    public void openSocketGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§l宝石镶嵌");
        
        // 设置界面布局
        setupGUILayout(gui);
        
        player.openInventory(gui);
    }

    /**
     * 设置GUI界面布局
     * @param gui 界面库存
     */
    public void setupGUILayout(Inventory gui) {
        // 设置边框 - 使用缓存的边框物品
        for (int i = 0; i < 54; i++) {
            if (isSlotBorder(i)) {
                gui.setItem(i, cachedBorderItem);
            }
        }
        
        // 设置功能槽位 - 使用缓存的物品
        gui.setItem(EQUIPMENT_SLOT, cachedEquipmentHint);
        gui.setItem(GEM_SLOT, cachedGemHint);
        gui.setItem(RESULT_SLOT, cachedResultHint);
        gui.setItem(CONFIRM_SLOT, cachedConfirmButton);
        gui.setItem(CANCEL_SLOT, cachedCancelButton);
        gui.setItem(UNSOCKET_SLOT, cachedUnsocketButton);
    }

    /**
     * 检查槽位是否为边框
     * @param slot 槽位索引
     * @return 是否为边框
     */
    private boolean isSlotBorder(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        
        // 第一行和最后一行
        if (row == 0 || row == 5) {
            return true;
        }
        
        // 第一列和最后一列（除了功能槽位）
        if (col == 0 || col == 8) {
            return slot != EQUIPMENT_SLOT && slot != GEM_SLOT && slot != RESULT_SLOT 
                   && slot != CONFIRM_SLOT && slot != CANCEL_SLOT && slot != UNSOCKET_SLOT;
        }
        
        return false;
    }

    /**
     * 创建GUI物品
     * @param material 材质
     * @param displayName 显示名称
     * @param lore 描述
     * @return 物品
     */
    private ItemStack createGuiItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
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
     * 更新镶嵌预览
     * @param gui 界面库存
     * @param equipment 装备物品
     * @param gem 宝石物品
     */
    public void updateSocketPreview(Inventory gui, ItemStack equipment, ItemStack gem) {
        // 检查输入是否为GUI物品
        boolean isEquipmentReal = equipment != null && !equipment.getType().isAir() && !isGUIItem(equipment);
        boolean isGemReal = gem != null && !gem.getType().isAir() && !isGUIItem(gem) && gemManager.isGem(gem);
        
        if (!isEquipmentReal || !isGemReal) {
            // 使用缓存的结果提示物品
            gui.setItem(RESULT_SLOT, cachedResultHint);
            return;
        }

        // 检查装备是否已镶嵌宝石（添加上限检查）
        if (gemManager.hasSocketedGem(equipment)) {
            ItemStack limitResult = createGuiItem(Material.BARRIER, 
                "§c已达上限", 
                List.of("§7该装备已镶嵌宝石", "§7请先拆卸现有宝石"));
            gui.setItem(RESULT_SLOT, limitResult);
            return;
        }

        // 检查宝石是否可以镶嵌在此装备上
        GemInstance gemInstance = gemManager.getGemInstance(gem);
        GemData gemData = gemManager.getGemData(gemInstance.getGemType());
        
        if (gemData == null || !gemData.canSocketOn(equipment.getType())) {
            // 使用缓存的失败结果物品
            gui.setItem(RESULT_SLOT, cachedFailureResult);
            return;
        }

        // 创建预览物品
        ItemStack preview = equipment.clone();
        ItemMeta previewMeta = preview.getItemMeta();
        if (previewMeta != null) {
            List<String> lore = previewMeta.hasLore() ? new ArrayList<>(previewMeta.getLore()) : new ArrayList<>();
            
            // 添加宝石信息到描述中
            lore.add("§7");
            lore.add("§6§l镶嵌的宝石:");
            lore.add("  §e" + gemData.getDisplayName() + " §7[Lv." + gemInstance.getLevel() + "]");
            
            // 添加宝石属性
            gemInstance.getAttributes().forEach((type, value) -> {
                String attrName = getAttributeName(gemData, type);
                lore.add(String.format("    §7%s: §a+%.1f", attrName, value));
            });
            
            previewMeta.setLore(lore);
            preview.setItemMeta(previewMeta);
        }

        gui.setItem(RESULT_SLOT, preview);
    }

    /**
     * 获取属性显示名称
     */
    private String getAttributeName(GemData gemData, String attributeType) {
        for (var attr : gemData.getAttributes()) {
            if (attr.getType().equals(attributeType)) {
                return attr.getName();
            }
        }
        return attributeType;
    }

    /**
     * 执行镶嵌操作
     * @param player 玩家
     * @param equipment 装备物品
     * @param gem 宝石物品
     * @return 镶嵌后的装备，失败返回null
     */
    public ItemStack performSocket(Player player, ItemStack equipment, ItemStack gem) {
        if (equipment == null || gem == null || !gemManager.isGem(gem)) {
            player.sendMessage("§c镶嵌失败：物品无效");
            return null;
        }

        // 检查装备是否已经镶嵌了宝石
        if (gemManager.hasSocketedGem(equipment)) {
            player.sendMessage("§c镶嵌失败：该装备已经镶嵌了宝石！每个装备只能镶嵌一颗宝石");
            return null;
        }

        GemInstance gemInstance = gemManager.getGemInstance(gem);
        GemData gemData = gemManager.getGemData(gemInstance.getGemType());
        
        if (gemData == null) {
            player.sendMessage("§c镶嵌失败：无法识别的宝石");
            return null;
        }

        if (!gemData.canSocketOn(equipment.getType())) {
            SocketPosition position = SocketPosition.getPositionByMaterial(equipment.getType());
            String positionName = position != null ? position.getDisplayName() : "此装备";
            player.sendMessage("§c镶嵌失败：" + gemData.getDisplayName() + " §c无法镶嵌在" + positionName + "上");
            return null;
        }

        // 使用GemManager的方法执行镶嵌
        ItemStack result = gemManager.socketGemToEquipment(equipment, gemInstance);
        
        if (result != null) {
            player.sendMessage("§a镶嵌成功！" + gemData.getDisplayName() + " §a已镶嵌到装备上");
            
            // 仅消耗1枚宝石，返还余量
            if (gem.getAmount() > 1) {
                ItemStack remain = gem.clone();
                remain.setAmount(gem.getAmount() - 1);
                player.getInventory().addItem(remain);
            }
        } else {
            player.sendMessage("§c镶嵌失败：处理过程中出现错误");
        }
        
        return result;
    }

    /**
     * 执行拆卸操作
     * @param player 玩家
     * @param equipment 装备物品
     * @return 拆卸的宝石物品，失败返回null
     */
    public ItemStack performUnsocket(Player player, ItemStack equipment) {
        if (equipment == null || equipment.getType().isAir()) {
            player.sendMessage("§c拆卸失败：请先放入装备");
            return null;
        }

        if (!gemManager.hasSocketedGem(equipment)) {
            player.sendMessage("§c拆卸失败：该装备未镶嵌任何宝石");
            return null;
        }

        // 拆卸宝石
        ItemStack gem = gemManager.unsocketGemFromEquipment(equipment);
        if (gem != null) {
            player.sendMessage("§a拆卸成功！已获得宝石");
            return gem;
        } else {
            player.sendMessage("§c拆卸失败：处理过程中出现错误");
            return null;
        }
    }

    /**
     * 检查槽位是否为功能槽位
     * @param slot 槽位索引
     * @return 是否为功能槽位
     */
    public boolean isFunctionalSlot(int slot) {
        return slot == EQUIPMENT_SLOT || slot == GEM_SLOT || slot == RESULT_SLOT 
               || slot == CONFIRM_SLOT || slot == CANCEL_SLOT || slot == UNSOCKET_SLOT;
    }

    /**
     * 检查物品是否为GUI物品
     */
    private boolean isGUIItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
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
               "§c已达上限".equals(displayName) ||
               "§7".equals(displayName) ||
               item.getType().name().contains("GLASS_PANE");
    }

    /**
     * 获取缓存的GUI物品 - 性能优化
     */
    public ItemStack getCachedEquipmentHint() { return cachedEquipmentHint; }
    public ItemStack getCachedGemHint() { return cachedGemHint; }
    public ItemStack getCachedResultHint() { return cachedResultHint; }

    /**
     * 获取各个功能槽位的索引
     */
    public static int getEquipmentSlot() { return EQUIPMENT_SLOT; }
    public static int getGemSlot() { return GEM_SLOT; }
    public static int getResultSlot() { return RESULT_SLOT; }
    public static int getConfirmSlot() { return CONFIRM_SLOT; }
    public static int getCancelSlot() { return CANCEL_SLOT; }
    public static int getUnsocketSlot() { return UNSOCKET_SLOT; }
}
