package com.ximangge.xrail;

import com.ximangge.xrail.command.XRailCommand;
import com.ximangge.xrail.config.ConfigManager;
import com.ximangge.xrail.display.DisplayManager;
import com.ximangge.xrail.gui.SpeedGUI;
import com.ximangge.xrail.language.LanguageManager;
import com.ximangge.xrail.listener.CartListener;
import com.ximangge.xrail.listener.GuiListener;
import com.ximangge.xrail.listener.InteractListener;
import com.ximangge.xrail.speed.SpeedManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * XRail - Minecraft Paper/Purpur 矿车速度控制插件。
 *
 * 功能：
 * 1. 按矿车类型修改速度上限（最高 75 格/秒，命令方块矿车除外）
 * 2. 潜行 + 左键 + 右键矿车打开箱子界面设置速度（钻石 ±5 / 红石粉 ±1，双页切换）
 * 3. /xrail 命令：全局设置、原版切换、语言切换、重载
 * 4. 实时速度显示栏（动作条 / Boss 条 / 聊天栏，3 位小数）
 * 5. 首次运行生成 config.yml 与 lang/ 语言文件夹
 */
public final class XRail extends JavaPlugin {

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private SpeedManager speedManager;
    private DisplayManager displayManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        languageManager = new LanguageManager(this);
        languageManager.ensureDefaults();
        languageManager.load(configManager.getLanguage());

        speedManager = new SpeedManager(this);

        displayManager = new DisplayManager(this);
        displayManager.load();

        Bukkit.getPluginManager().registerEvents(new CartListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(displayManager, this);

        XRailCommand command = new XRailCommand(this);
        getCommand("xrail").setExecutor(command);
        getCommand("xrail").setTabCompleter(command);
        // 独立重载命令 /xrailreload（等价于 /xrail reload）
        getCommand("xrailreload").setExecutor((sender, cmd, label, args) -> {
            if (!sender.hasPermission("xrail.admin")) {
                languageManager.send(sender, "no-permission");
                return true;
            }
            reloadAll();
            languageManager.send(sender, "reload-done");
            return true;
        });

        speedManager.applyAllLoaded();
        speedManager.startSweepTask();
        displayManager.startTask();

        getLogger().info("XRail enabled. language=" + configManager.getLanguage());
    }

    @Override
    public void onDisable() {
        // 先关闭本插件的设置界面：热禁用时监听器即将失效，防止界面物品被取走
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof SpeedGUI) {
                player.closeInventory();
            }
        }
        Bukkit.getScheduler().cancelTasks(this);
        if (displayManager != null) {
            displayManager.cleanup();
        }
        getLogger().info("XRail disabled.");
    }

    /** /xrail reload（或 /xrailreload）：重载配置、语言、玩家数据显示设置，并重新应用速度 */
    public void reloadAll() {
        configManager.load();
        languageManager.ensureDefaults();
        languageManager.load(configManager.getLanguage());
        // 重新读取 playerdata.yml：手动改动过的显示方式无需重启服务器即可生效
        displayManager.load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            displayManager.removeBossBar(player); // 显示方式可能已变，旧的 Boss 条需要重建
        }
        speedManager.applyAllLoaded();
        displayManager.restartTask();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public SpeedManager getSpeedManager() {
        return speedManager;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }
}
