package com.lymc.gemrelic.command;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.manager.RelicManager;
import com.lymc.gemrelic.gui.*;
import com.lymc.gemrelic.relic.*;
import com.lymc.gemrelic.storage.DataMigration;
import com.lymc.gemrelic.storage.IRelicProfileManager;
import com.lymc.gemrelic.util.RelicItemConverter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RelicCommand implements CommandExecutor, TabCompleter {
    private final GemRelicPlugin plugin;
    private final RelicManager relicManager;

    public RelicCommand(GemRelicPlugin plugin) {
        this.plugin = plugin;
        this.relicManager = plugin.getRelicManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6/relic list §7- 列出已配置的套装");
            sender.sendMessage("§6/relic reload §7- 重载圣遗物配置");
            sender.sendMessage("§6/relic open §7- 打开圣遗物界面");
            sender.sendMessage("§6/relic equip <slot> <setId> §7- 穿戴（测试）");
            sender.sendMessage("§6/relic unequip <slot> §7- 卸下（测试）");
            sender.sendMessage("§6/relic test §7- 添加测试圣遗物");
            if (sender.hasPermission("gemrelic.admin")) {
                sender.sendMessage("§c=== 管理员命令 ===");
                sender.sendMessage("§6/relic migrate §7- 数据迁移到独立存储系统");
                sender.sendMessage("§6/relic migration-status §7- 查看迁移状态");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list":
                sender.sendMessage("§6§l==== 已配置套装 ====");
                if (relicManager.getRelicSetIds().isEmpty()) {
                    sender.sendMessage("§7暂无");
                } else {
                    relicManager.getRelicSetIds().forEach(id -> sender.sendMessage("§e- " + id));
                }
                return true;
            case "reload":
                if (!sender.hasPermission("gemrelic.admin")) {
                    sender.sendMessage("§c无权限");
                    return true;
                }
                plugin.reloadRelicConfig();
                sender.sendMessage("§a圣遗物配置已重载");
                return true;
            case "open":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c仅玩家可用");
                    return true;
                }
                new RelicMainMenuGUI(plugin).open((Player) sender);
                return true;
            case "equip":
                if (!(sender instanceof Player)) { sender.sendMessage("§c仅玩家可用"); return true; }
                if (args.length < 3) { sender.sendMessage("§c用法: /relic equip <slot> <setId>"); return true; }
                RelicSlot slot;
                try { slot = RelicSlot.valueOf(args[1].toUpperCase()); } catch (Exception e) { sender.sendMessage("§c无效slot"); return true; }
                String setId = args[2];
                if (plugin.getRelicManager().getRelicSet(setId) == null) { sender.sendMessage("§c未知套装"); return true; }
                Player p = (Player) sender;
                IRelicProfileManager pm = plugin.getRelicProfileManager();
                PlayerRelicProfile prof = pm.get(p);
                // 临时创建一件1级圣遗物用于测试
                RelicData data = new RelicData(java.util.UUID.randomUUID(), setId, slot, RelicRarity.GOLD, 0, 0,
                        new RelicMainStat(RelicStatType.ATK_PCT, 4.7), java.util.List.of(new RelicSubstat(RelicStatType.CRIT_RATE, 3.1)), false);
                prof.equip(data);
                plugin.getRelicEffectService().refresh(p, prof);
                plugin.getRelicProfileManager().save(p);
                sender.sendMessage("§a已穿戴: " + setId + " 到 " + slot);
                return true;
            case "unequip":
                if (!(sender instanceof Player)) { sender.sendMessage("§c仅玩家可用"); return true; }
                if (args.length < 2) { sender.sendMessage("§c用法: /relic unequip <slot>"); return true; }
                try {
                    RelicSlot s = RelicSlot.valueOf(args[1].toUpperCase());
                    Player pp = (Player) sender;
                    PlayerRelicProfile pr = plugin.getRelicProfileManager().get(pp);
                    pr.unequip(s);
                    plugin.getRelicEffectService().refresh(pp, pr);
                    plugin.getRelicProfileManager().save(pp);
                    sender.sendMessage("§a已卸下: " + s);
                } catch (Exception ex) {
                    sender.sendMessage("§c无效slot");
                }
                return true;
            case "test":
                if (!(sender instanceof Player)) { sender.sendMessage("§c仅玩家可用"); return true; }
                Player tp = (Player) sender;
                PlayerRelicProfile tpr = plugin.getRelicProfileManager().get(tp);
                RelicItemConverter converter = plugin.getRelicItemConverter();
                
                // 添加几个测试圣遗物到仓库
                for (RelicSlot testSlot : RelicSlot.values()) {
                    RelicData test = new RelicData(
                        UUID.randomUUID(), "gladiator", testSlot, RelicRarity.GOLD, 20, 0,
                        new RelicMainStat(RelicStatType.ATK_PCT, 46.6),
                        List.of(
                            new RelicSubstat(RelicStatType.CRIT_RATE, 7.8),
                            new RelicSubstat(RelicStatType.CRIT_DMG, 14.0),
                            new RelicSubstat(RelicStatType.ATK_SPEED, 4.2)
                        ),
                        false
                    );
                    tpr.addToWarehouse(test);
                }
                
                // 同时在背包中添加几个测试圣遗物物品（用于测试拖动功能）
                for (int i = 0; i < 3; i++) {
                    RelicData bagTest = new RelicData(
                        UUID.randomUUID(), "gladiator", RelicSlot.FLOWER, RelicRarity.PURPLE, 15, 0,
                        new RelicMainStat(RelicStatType.HP_FLAT, 4780),
                        List.of(
                            new RelicSubstat(RelicStatType.CRIT_RATE, 6.2),
                            new RelicSubstat(RelicStatType.ATK_PCT, 9.9)
                        ),
                        false
                    );
                    tp.getInventory().addItem(converter.toItemStack(bagTest));
                }
                
                // 立即保存到文件
                plugin.getRelicProfileManager().save(tp);
                sender.sendMessage("§a已添加5件测试圣遗物到仓库，3件到背包");
                sender.sendMessage("§e现在可以测试：拖动、Shift-点击等功能");
                return true;
            case "migrate":
                if (!sender.hasPermission("gemrelic.admin")) {
                    sender.sendMessage("§c无权限");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c仅玩家可用");
                    return true;
                }
                
                Player migrationPlayer = (Player) sender;
                DataMigration migration = new DataMigration(plugin);
                
                if (!migration.needsMigration(migrationPlayer)) {
                    sender.sendMessage("§e您的数据不需要迁移或已经迁移完成");
                    return true;
                }
                
                sender.sendMessage("§e开始迁移您的圣遗物数据到独立存储系统...");
                sender.sendMessage("§7注意：新系统类似末影箱原理，但不占用您的末影箱空间");
                
                // 备份旧数据
                if (migration.backupOldData(migrationPlayer)) {
                    sender.sendMessage("§a已备份原始数据");
                } else {
                    sender.sendMessage("§c备份失败，取消迁移");
                    return true;
                }
                
                // 执行迁移
                boolean success = migration.migratePlayer(migrationPlayer);
                if (success) {
                    sender.sendMessage("§a迁移成功！您的圣遗物现在存储在独立存储系统中");
                    sender.sendMessage("§e数据更加安全可靠，采用类似末影箱的存储机制");
                } else {
                    sender.sendMessage("§c迁移失败，请检查存储空间");
                }
                return true;
                
            case "migration-status":
                if (!sender.hasPermission("gemrelic.admin")) {
                    sender.sendMessage("§c无权限");
                    return true;
                }
                
                DataMigration statusMigration = new DataMigration(plugin);
                sender.sendMessage("§6=== 数据迁移状态 ===");
                sender.sendMessage(statusMigration.getMigrationStats());
                
                if (sender instanceof Player) {
                    Player statusPlayer = (Player) sender;
                    if (statusMigration.needsMigration(statusPlayer)) {
                        sender.sendMessage("§e您的数据需要迁移：使用 §6/relic migrate §e命令");
                    } else {
                        sender.sendMessage("§a您的数据已迁移或无需迁移");
                    }
                }
                return true;
                
            default:
                sender.sendMessage("§c未知子命令");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 第一层：子命令补全
        if (args.length == 1) {
            completions.addAll(List.of("list", "open", "test", "equip", "unequip"));
            if (sender.hasPermission("gemrelic.admin")) {
                completions.addAll(List.of("reload", "migrate", "migration-status"));
            }
            return filterCompletions(completions, args[0]);
        }
        
        // 第二层：参数补全
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            // equip 和 unequip 命令补全部位参数
            if (subCommand.equals("equip") || subCommand.equals("unequip")) {
                completions.addAll(List.of("FLOWER", "PLUME", "SANDS", "GOBLET", "CIRCLET"));
                return filterCompletions(completions, args[1]);
            }
        }
        
        // 第三层：套装ID补全（仅 equip 命令）
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("equip")) {
                // 补全套装ID
                completions.addAll(relicManager.getRelicSetIds());
                return filterCompletions(completions, args[2]);
            }
        }
        
        return completions;
    }
    
    /**
     * 过滤补全结果
     */
    private List<String> filterCompletions(List<String> completions, String input) {
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}


