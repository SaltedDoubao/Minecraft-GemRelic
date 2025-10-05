package com.lymc.gemrelic.command;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.manager.RelicManager;
import com.lymc.gemrelic.gui.*;
import com.lymc.gemrelic.util.RelicItemConverter;
import com.lymc.gemrelic.manager.RelicProfileManager;
import com.lymc.gemrelic.relic.*;
import org.bukkit.Bukkit;
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
                RelicProfileManager pm = plugin.getRelicProfileManager();
                PlayerRelicProfile prof = pm.get(p);
                // 临时创建一件1级圣遗物用于测试
                RelicData data = new RelicData(java.util.UUID.randomUUID(), setId, slot, RelicRarity.GOLD, 0, 0,
                        new RelicMainStat(RelicStatType.ATK_PCT, 4.7), java.util.List.of(new RelicSubstat(RelicStatType.CRIT_RATE, 3.1)), false);
                prof.equip(data);
                plugin.getRelicEffectService().refresh(p, prof);
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
                    sender.sendMessage("§a已卸下: " + s);
                } catch (Exception ex) {
                    sender.sendMessage("§c无效slot");
                }
                return true;
            case "test":
                if (!(sender instanceof Player)) { sender.sendMessage("§c仅玩家可用"); return true; }
                Player tp = (Player) sender;
                PlayerRelicProfile tpr = plugin.getRelicProfileManager().get(tp);
                
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
                sender.sendMessage("§a已添加5件测试圣遗物到仓库");
                return true;
            default:
                sender.sendMessage("§c未知子命令");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> res = new ArrayList<>();
        if (args.length == 1) {
            res.add("list");
            if (sender.hasPermission("gemrelic.admin")) res.add("reload");
            return res.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return res;
    }
}


