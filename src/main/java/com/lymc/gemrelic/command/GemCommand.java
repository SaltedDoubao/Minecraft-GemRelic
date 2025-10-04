package com.lymc.gemrelic.command;

import com.lymc.gemrelic.GemRelicPlugin;
import com.lymc.gemrelic.manager.GemManager;
import com.lymc.gemrelic.model.GemInstance;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gem 命令处理器
 * 处理 /gemrelic 相关的所有子命令
 */
public class GemCommand implements CommandExecutor, TabCompleter {
    
    private final GemRelicPlugin plugin;
    private final GemManager gemManager;

    public GemCommand(GemRelicPlugin plugin) {
        this.plugin = plugin;
        this.gemManager = plugin.getGemManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                return handleGive(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "reload":
                return handleReload(sender);
            case "list":
                return handleList(sender);
            case "help":
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    /**
     * 处理 give 子命令
     * 用法: /gemrelic give <玩家> <宝石类型> [等级]
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gemrelic.command.gemrelic.give")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c用法: /gemrelic give <玩家> <宝石类型> [等级]");
            return true;
        }

        String playerName = args[1];
        String gemType = args[2];
        int level = 1;

        if (args.length >= 4) {
            try {
                level = Integer.parseInt(args[3]);
                if (level < 1) {
                    sender.sendMessage("§c等级必须大于0！");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c等级必须是一个数字！");
                return true;
            }
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§c玩家 " + playerName + " 不在线或不存在！");
            return true;
        }

        ItemStack gem = gemManager.createGem(gemType, level);
        if (gem == null) {
            sender.sendMessage("§c未找到宝石类型: " + gemType);
            sender.sendMessage("§7使用 /gemrelic list 查看可用的宝石类型");
            return true;
        }

        target.getInventory().addItem(gem);
        target.sendMessage("§a你获得了一颗 " + gemType + " §a宝石！");
        sender.sendMessage("§a成功给予 " + target.getName() + " 一颗 " + gemType + " 宝石（等级 " + level + "）");
        
        return true;
    }

    /**
     * 处理 info 子命令
     * 用法: /gemrelic info
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            player.sendMessage("§c请手持一个宝石！");
            return true;
        }

        if (!gemManager.isGem(item)) {
            player.sendMessage("§c你手中的物品不是宝石！");
            return true;
        }

        GemInstance gemInstance = gemManager.getGemInstance(item);
        if (gemInstance == null) {
            player.sendMessage("§c无法读取宝石信息！");
            return true;
        }

        player.sendMessage("§6§l========== 宝石信息 ==========");
        player.sendMessage("§e宝石类型: §f" + gemInstance.getGemType());
        player.sendMessage("§e宝石等级: §f" + gemInstance.getLevel());
        player.sendMessage("§e属性:");
        gemInstance.getAttributes().forEach((type, value) -> 
            player.sendMessage(String.format("  §7- §a%s: §f%.2f", type, value))
        );
        player.sendMessage("§6§l============================");

        return true;
    }

    /**
     * 处理 reload 子命令
     * 用法: /gemrelic reload
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("gemrelic.admin")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }

        sender.sendMessage("§7正在重载配置...");
        plugin.reloadPluginConfig();
        sender.sendMessage("§a配置重载成功！");
        
        return true;
    }

    /**
     * 处理 list 子命令
     * 用法: /gemrelic list
     */
    private boolean handleList(CommandSender sender) {
        sender.sendMessage("§6§l========== 可用宝石类型 ==========");
        if (gemManager.getGemTypes().isEmpty()) {
            sender.sendMessage("§c未找到任何宝石类型！");
        } else {
            gemManager.getGemTypes().forEach(type -> 
                sender.sendMessage("  §7- §e" + type)
            );
        }
        sender.sendMessage("§6§l================================");
        return true;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6§l========== GemRelic 帮助 ==========");
        sender.sendMessage("§e/gemrelic give <玩家> <类型> [等级] §7- 给予宝石");
        sender.sendMessage("§e/gemrelic info §7- 查看手持宝石信息");
        sender.sendMessage("§e/gemrelic list §7- 列出所有宝石类型");
        sender.sendMessage("§e/gemrelic reload §7- 重载配置（需要管理员权限）");
        sender.sendMessage("§e/gemrelic help §7- 显示此帮助信息");
        sender.sendMessage("§6§l==================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 第一级子命令补全
            List<String> subCommands = Arrays.asList("give", "info", "list", "reload", "help");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // 补全在线玩家名
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // 补全宝石类型
            return gemManager.getGemTypes().stream()
                    .filter(type -> type.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            // 补全等级建议
            return Arrays.asList("1", "2", "3", "5", "10");
        }

        return completions;
    }
}

