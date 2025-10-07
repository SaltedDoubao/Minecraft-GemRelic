package com.salteddoubao.relicsystem.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;
import com.salteddoubao.relicsystem.gui.*;
import com.salteddoubao.relicsystem.manager.RelicManager;
import com.salteddoubao.relicsystem.relic.*;
import com.salteddoubao.relicsystem.storage.DataMigration;
import com.salteddoubao.relicsystem.storage.IRelicProfileManager;
import com.salteddoubao.relicsystem.util.RelicItemConverter;
import com.salteddoubao.relicsystem.service.RelicGenerationService;

import java.util.*;
import java.util.stream.Collectors;

public class RelicCommand implements CommandExecutor, TabCompleter {
    private final MinecraftRelicSystem plugin;
    private final RelicManager relicManager;

    public RelicCommand(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
        this.relicManager = plugin.getRelicManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6/relic help §7- 查看命令帮助");
            sender.sendMessage("§6/relic list [page] §7- 列出已配置的套装（支持翻页）");
            sender.sendMessage("§6/relic reload §7- 重载圣遗物配置");
            sender.sendMessage("§6/relic open §7- 打开圣遗物界面");
            if (sender.hasPermission("mrs.admin")) {
                sender.sendMessage("§c=== 管理员命令 ===");
                sender.sendMessage("§6/relic equip <slot> <setId> §7- 穿戴（测试）");
                sender.sendMessage("§6/relic unequip <slot> §7- 卸下（测试）");
                sender.sendMessage("§6/relic test §7- 添加测试圣遗物");
                sender.sendMessage("§6/relic box give <玩家> <宝箱id> [数量] §7- 发放宝箱");
                sender.sendMessage("§6/relic migrate §7- 数据迁移到独立存储系统");
                sender.sendMessage("§6/relic migration-status §7- 查看迁移状态");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                sender.sendMessage("§6§l==== Relic 命令帮助 ====");
                sender.sendMessage("§e基础命令:");
                sender.sendMessage("§6/relic help §7- 查看命令帮助");
                sender.sendMessage("§6/relic list [page] §7- 列出套装（翻页）");
                sender.sendMessage("§6/relic open §7- 打开圣遗物界面");
                if (sender.hasPermission("mrs.admin")) {
                    sender.sendMessage("§c管理员命令:");
                    sender.sendMessage("§6/relic reload §7- 重载圣遗物配置");
                    sender.sendMessage("§6/relic gen <setId> <slot> <rarity> <level> §7- 生成并发放到背包");
                    sender.sendMessage("§6/relic give <player> <setId> <slot> <level> §7- 发放圣遗物");
                    sender.sendMessage("§6/relic equip <slot> <setId> §7- 穿戴（测试）");
                    sender.sendMessage("§6/relic unequip <slot> §7- 卸下（测试）");
                    sender.sendMessage("§6/relic test §7- 发放测试圣遗物");
                    sender.sendMessage("§6/relic box give <player> <boxId> [amount] §7- 发放宝箱");
                    sender.sendMessage("§6/relic migrate §7- 迁移旧数据");
                    sender.sendMessage("§6/relic migration-status §7- 查看迁移状态");
                }
                return true;
            case "list":
                {
                    java.util.List<String> ids = new java.util.ArrayList<>(relicManager.getRelicSetIds());
                    if (ids.isEmpty()) { sender.sendMessage("§7暂无套装"); return true; }
                    // 排序：按中文名（若无则用ID）
                    ids.sort(java.util.Comparator.comparing(id -> {
                        com.salteddoubao.relicsystem.relic.RelicSet s = relicManager.getRelicSet(id);
                        return s != null ? s.getName() : id;
                    }));

                    int pageSize = 6;
                    int total = ids.size();
                    int totalPages = (total + pageSize - 1) / pageSize;
                    int page = 1;
                    if (args.length >= 2) { try { page = Integer.parseInt(args[1]); } catch (Exception ignore) {} }
                    if (page < 1) page = 1;
                    if (page > totalPages) page = totalPages;

                    int from = (page - 1) * pageSize;
                    int to = Math.min(from + pageSize, total);
                    sender.sendMessage("§6§l==== 已配置套装 §e" + page + "§6/§e" + totalPages + " §7(共" + total + ") ====");
                    for (int i = from; i < to; i++) {
                        String id = ids.get(i);
                        com.salteddoubao.relicsystem.relic.RelicSet s = relicManager.getRelicSet(id);
                        String name = s != null ? s.getName() : id;
                        String four = (s != null && !s.getFourPieceEffects().isEmpty()) ? s.getFourPieceEffects().get(0) : "";
                        String preview = four.length() > 28 ? four.substring(0, 28) + "..." : four;
                        sender.sendMessage("§e- " + name + " §7(" + id + ") §f| §b四件套: §7" + preview);
                    }
                    if (page < totalPages) sender.sendMessage("§7下一页: §f/relic list " + (page + 1) + " §8(每页" + pageSize + "条)");
                    return true;
                }
            case "reload":
                if (!sender.hasPermission("mrs.admin")) {
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
            case "gen":
                if (!sender.hasPermission("mrs.admin")) { sender.sendMessage("§c无权限"); return true; }
                if (!(sender instanceof Player)) { sender.sendMessage("§c仅玩家可用"); return true; }
                if (args.length < 5) {
                    sender.sendMessage("§c用法: /relic gen <套装id> <部位> <稀有度> <等级>");
                    return true;
                }
                String genSet = args[1];
                if (plugin.getRelicManager().getRelicSet(genSet) == null) { sender.sendMessage("§c未知套装: " + genSet); return true; }
                RelicSlot genSlot;
                try { genSlot = RelicSlot.valueOf(args[2].toUpperCase()); } catch (Exception ex) { sender.sendMessage("§c无效部位"); return true; }
                RelicRarity genRarity;
                try { genRarity = RelicRarity.valueOf(args[3].toUpperCase()); } catch (Exception ex) { sender.sendMessage("§c无效稀有度"); return true; }
                int genLevel;
                try { genLevel = Integer.parseInt(args[4]); } catch (Exception ex) { sender.sendMessage("§c等级必须是数字"); return true; }
                if (genLevel < 0) genLevel = 0;

                Player genPlayer = (Player) sender;
                RelicGenerationService genSvc = plugin.getRelicGenerationService();
                RelicData genData = genSvc.generate(genSet, genSlot, genRarity, genLevel);
                RelicItemConverter genConv = plugin.getRelicItemConverter();
                genPlayer.getInventory().addItem(genConv.toItemStack(genData));
                sender.sendMessage("§a已生成并发放: " + genSet + " - " + genSlot + " (" + genRarity + ", Lv." + genData.getLevel() + ")");
                return true;
            case "equip":
                if (!sender.hasPermission("mrs.admin")) { sender.sendMessage("§c无权限"); return true; }
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
                if (!sender.hasPermission("mrs.admin")) { sender.sendMessage("§c无权限"); return true; }
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
                if (!sender.hasPermission("mrs.admin")) { sender.sendMessage("§c无权限"); return true; }
                if (!(sender instanceof Player)) { sender.sendMessage("§c仅玩家可用"); return true; }
                Player tp = (Player) sender;
                RelicItemConverter converter = plugin.getRelicItemConverter();

                // 清理：不向仓库添加，全部发放至背包
                int given = 0;
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
                    tp.getInventory().addItem(converter.toItemStack(test));
                    given++;
                }

                // 额外随机3件发放至背包
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
                    given++;
                }

                sender.sendMessage("§a已向背包发放 " + given + " 件测试圣遗物");
                return true;
            case "give":
                // /relic give [玩家名] [套装id] [装备位置] [等级]
                if (!sender.hasPermission("mrs.admin")) { sender.sendMessage("§c无权限"); return true; }
                if (args.length < 5) { sender.sendMessage("§c用法: /relic give <玩家名> <套装id> <部位> <等级>"); return true; }
                String targetName = args[1];
                org.bukkit.entity.Player target = plugin.getServer().getPlayerExact(targetName);
                if (target == null) { sender.sendMessage("§c玩家不在线: " + targetName); return true; }
                String giveSetId = args[2];
                if (plugin.getRelicManager().getRelicSet(giveSetId) == null) { sender.sendMessage("§c未知套装: " + giveSetId); return true; }
                RelicSlot giveSlot;
                try { giveSlot = RelicSlot.valueOf(args[3].toUpperCase()); } catch (Exception ex) { sender.sendMessage("§c无效部位"); return true; }
                int level;
                try { level = Integer.parseInt(args[4]); } catch (NumberFormatException ex) { sender.sendMessage("§c等级必须是数字"); return true; }
                if (level < 0) level = 0;
                if (level > 20) level = 20;

                RelicItemConverter conv = plugin.getRelicItemConverter();
                // 简化：根据部位选一个常用主词条
                RelicMainStat main;
                switch (giveSlot) {
                    case FLOWER -> main = new RelicMainStat(RelicStatType.HP_FLAT, 4780);
                    case PLUME -> main = new RelicMainStat(RelicStatType.ATK_FLAT, 311);
                    case SANDS -> main = new RelicMainStat(RelicStatType.ATK_PCT, 46.6);
                    case GOBLET -> main = new RelicMainStat(RelicStatType.ELEM_DMG_ANY, 46.6);
                    case CIRCLET -> main = new RelicMainStat(RelicStatType.CRIT_RATE, 31.1);
                    default -> main = new RelicMainStat(RelicStatType.ATK_PCT, 46.6);
                }
                RelicData grant = new RelicData(UUID.randomUUID(), giveSetId, giveSlot, RelicRarity.GOLD, level, 0,
                        main, List.of(new RelicSubstat(RelicStatType.CRIT_RATE, 3.1), new RelicSubstat(RelicStatType.ATK_PCT, 5.8)), false);
                target.getInventory().addItem(conv.toItemStack(grant));
                sender.sendMessage("§a已发放圣遗物给 " + target.getName());
                if (!sender.equals(target)) { target.sendMessage("§a收到管理员发放的圣遗物: " + giveSetId + " - " + giveSlot); }
                return true;
            case "box":
                if (!sender.hasPermission("mrs.admin")) { sender.sendMessage("§c无权限"); return true; }
                if (args.length < 2) { sender.sendMessage("§c用法: /relic box <subcmd>"); return true; }
                String bsub = args[1].toLowerCase();
                if (bsub.equals("give")) {
                    if (args.length < 4) { sender.sendMessage("§c用法: /relic box give <玩家> <宝箱id> [数量]"); return true; }
                    org.bukkit.entity.Player tgt = plugin.getServer().getPlayerExact(args[2]);
                    if (tgt == null) { sender.sendMessage("§c玩家不在线"); return true; }
                    String boxId = args[3];
                    if (plugin.getTreasureBoxManager().get(boxId) == null) { sender.sendMessage("§c未知宝箱: " + boxId); return true; }
                    int amount = 1;
                    if (args.length >= 5) { try { amount = Math.max(1, Integer.parseInt(args[4])); } catch (Exception ignore) {} }
                    com.salteddoubao.relicsystem.util.TreasureBoxItemFactory fac = new com.salteddoubao.relicsystem.util.TreasureBoxItemFactory(plugin);
                    com.salteddoubao.relicsystem.manager.TreasureBoxManager.BoxDef def = plugin.getTreasureBoxManager().get(boxId);
                    org.bukkit.inventory.ItemStack item = fac.create(boxId, def.displayName, def.lore, def.material, def.customModelData, def.glow);
                    item.setAmount(amount);
                    tgt.getInventory().addItem(item);
                    sender.sendMessage("§a已发放宝箱: " + boxId + " x" + amount);
                    if (!sender.equals(tgt)) tgt.sendMessage("§a收到管理员发放的宝箱: " + boxId + " x" + amount);
                    return true;
                }
                sender.sendMessage("§c未知子命令");
                return true;
            case "migrate":
                if (!sender.hasPermission("mrs.admin")) {
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
                if (!sender.hasPermission("mrs.admin")) {
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
            completions.addAll(List.of("help", "list", "open"));
            if (sender.hasPermission("mrs.admin")) {
                completions.addAll(List.of("test", "equip", "unequip", "give", "gen", "box", "reload", "migrate", "migration-status"));
            }
            return filterCompletions(completions, args[0]);
        }
        
        // 第二层：参数补全
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("list")) {
                completions.addAll(List.of("1", "2", "3", "4", "5"));
                return filterCompletions(completions, args[1]);
            }

            // equip 和 unequip 命令补全部位参数
            if (subCommand.equals("equip") || subCommand.equals("unequip")) {
                completions.addAll(List.of("FLOWER", "PLUME", "SANDS", "GOBLET", "CIRCLET"));
                return filterCompletions(completions, args[1]);
            }
            if (subCommand.equals("give")) {
                for (org.bukkit.entity.Player pl : plugin.getServer().getOnlinePlayers()) completions.add(pl.getName());
                return filterCompletions(completions, args[1]);
            }
            if (subCommand.equals("gen")) {
                // gen 的第二个参数是套装ID
                completions.addAll(relicManager.getRelicSetIds());
                return filterCompletions(completions, args[1]);
            }
            if (subCommand.equals("box")) {
                completions.add("give");
                return filterCompletions(completions, args[1]);
            }
        }
        
        // 第三层
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("equip")) {
                // 补全套装ID
                completions.addAll(relicManager.getRelicSetIds());
                return filterCompletions(completions, args[2]);
            }
            if (subCommand.equals("give")) {
                completions.addAll(relicManager.getRelicSetIds());
                return filterCompletions(completions, args[2]);
            }
            if (subCommand.equals("gen")) {
                // gen 的第三个参数是部位
                completions.addAll(List.of("FLOWER", "PLUME", "SANDS", "GOBLET", "CIRCLET"));
                return filterCompletions(completions, args[2]);
            }
            if (subCommand.equals("box") && args[1].equalsIgnoreCase("give")) {
                for (org.bukkit.entity.Player pl : plugin.getServer().getOnlinePlayers()) completions.add(pl.getName());
                return filterCompletions(completions, args[2]);
            }
        }
        
        // 第四层
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            // give 的第四个参数是部位
            completions.addAll(List.of("FLOWER", "PLUME", "SANDS", "GOBLET", "CIRCLET"));
            return filterCompletions(completions, args[3]);
        }
        
        // gen 的第四个参数是稀有度
        if (args.length == 4 && args[0].equalsIgnoreCase("gen")) {
            for (RelicRarity r : RelicRarity.values()) completions.add(r.name());
            return filterCompletions(completions, args[3]);
        }

        // box give 的第四个参数是宝箱ID
        if (args.length == 4 && args[0].equalsIgnoreCase("box") && args[1].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getTreasureBoxManager().ids());
            return filterCompletions(completions, args[3]);
        }

        if (args.length == 5 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("gen"))) {
            completions.addAll(List.of("0", "5", "10", "15", "20", "25", "30"));
            return filterCompletions(completions, args[4]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("box") && args[1].equalsIgnoreCase("give")) {
            completions.addAll(List.of("1", "8", "16", "32", "64"));
            return filterCompletions(completions, args[4]);
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


