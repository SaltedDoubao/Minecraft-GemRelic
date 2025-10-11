package com.salteddoubao.relicsystem.util;

import com.salteddoubao.relicsystem.MinecraftRelicSystem;

/**
 * 异常处理工具类
 */
public class ExceptionHandler {
    private final MinecraftRelicSystem plugin;
    
    public ExceptionHandler(MinecraftRelicSystem plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 记录非关键异常（仅在调试模式下）
     */
    public void logNonCritical(String context, Exception e) {
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().warning(context + ": " + e.getMessage());
        }
    }
    
    /**
     * 记录非关键异常（总是记录，但级别较低）
     */
    public void logMinor(String context, Exception e) {
        plugin.getLogger().fine(context + ": " + e.getMessage());
    }
    
    /**
     * 记录警告级别异常（总是记录）
     */
    public void logWarning(String context, Exception e) {
        plugin.getLogger().warning(context + ": " + e.getMessage());
        if (plugin.getConfig().getBoolean("settings.debug", false) && e.getCause() != null) {
            plugin.getLogger().warning("  原因: " + e.getCause().getMessage());
        }
    }
    
    /**
     * 记录严重异常（总是记录，包含堆栈）
     */
    public void logSevere(String context, Exception e) {
        plugin.getLogger().severe(context + ": " + e.getMessage());
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            e.printStackTrace();
        }
    }
    
    /**
     * 安全地执行操作，失败时记录但不抛出
     */
    public void safeExecute(String context, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            logNonCritical(context, e);
        }
    }
}

