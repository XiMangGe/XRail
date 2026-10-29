package com.ximangge.xrail.display;

import com.ximangge.xrail.XRail;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 实时速度显示栏。
 * 显示方式（玩家可用 /xrail display 自行切换，保存于 playerdata.yml）：
 * - ACTIONBAR：物品栏上方动作条，随刷新间隔实时更新
 * - BOSSBAR  ：屏幕上方 Boss 血条，进度条随速度/上限变化
 * - CHAT     ：聊天栏定期播报（间隔见 config 的 chat-interval）
 * - OFF      ：关闭
 * 速度统一保留 3 位小数（格/秒）。
 */
public class DisplayManager implements Listener {

    public enum Method {
        ACTIONBAR, BOSSBAR, CHAT, OFF;

        public static Method fromString(String s) {
            if (s == null) return null;
            switch (s.trim().toUpperCase(Locale.ROOT)) {
                case "ACTIONBAR":
                case "ACTION_BAR":
                case "AB":
                    return ACTIONBAR;
                case "BOSSBAR":
                case "BOSS_BAR":
                case "BB":
                    return BOSSBAR;
                case "CHAT":
                    return CHAT;
                case "OFF":
                    return OFF;
                default:
                    return null;
            }
        }

        public String lower() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final XRail plugin;
    private final File dataFile;
    private final Map<UUID, Method> methods = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Long> lastChat = new HashMap<>();
    private BukkitTask task;

    public DisplayManager(XRail plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
    }

    public void load() {
        methods.clear();
        if (!dataFile.isFile()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : yaml.getKeys(false)) {
            try {
                methods.put(UUID.fromString(key), Method.fromString(yaml.getString(key, "OFF")));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Method> e : methods.entrySet()) {
            yaml.set(e.getKey().toString(), e.getValue().name());
        }
        try {
            yaml.save(dataFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save playerdata.yml: " + ex.getMessage());
        }
    }

    /** 玩家当前的显示方式（服务器关闭显示栏功能时一律视为 OFF） */
    public Method methodOf(Player player) {
        if (!plugin.getConfigManager().isDisplayEnabled()) return Method.OFF;
        Method m = methods.get(player.getUniqueId());
        if (m == null) m = Method.fromString(plugin.getConfigManager().getDefaultMethod());
        return m != null ? m : Method.OFF;
    }

    public void setMethod(Player player, Method method) {
        methods.put(player.getUniqueId(), method);
        if (method != Method.BOSSBAR) {
            removeBossBar(player);
        }
        save();
    }

    public void startTask() {
        stopTask();
        long interval = plugin.getConfigManager().getUpdateInterval();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void restartTask() {
        startTask();
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long chatGapMs = plugin.getConfigManager().getChatInterval() * 50L;

        for (Player player : Bukkit.getOnlinePlayers()) {
            Method method = methodOf(player);
            if (method == Method.OFF || !(player.getVehicle() instanceof Minecart)) {
                removeBossBar(player);
                continue;
            }

            Minecart cart = (Minecart) player.getVehicle();
            double speed = cart.getVelocity().length() * 20.0D;
            String formatted = String.format(Locale.US, "%.3f", speed);

            switch (method) {
                case ACTIONBAR:
                    plugin.getLanguageManager().sendActionbar(player, "display.format", "%speed%", formatted);
                    break;

                case BOSSBAR: {
                    BossBar bar = bossBars.get(player.getUniqueId());
                    if (bar == null) {
                        bar = Bukkit.createBossBar("speed", BarColor.BLUE, BarStyle.SEGMENTED_10);
                        bar.addPlayer(player);
                        bossBars.put(player.getUniqueId(), bar);
                    }
                    // 值未变化时不重复发包
                    String title = plugin.getLanguageManager().format("display.format", "%speed%", formatted);
                    if (!bar.getTitle().equals(title)) {
                        bar.setTitle(title);
                    }
                    double progress = progressOf(speed, cart);
                    if (Math.abs(bar.getProgress() - progress) > 0.001D) {
                        bar.setProgress(progress);
                    }
                    break;
                }

                case CHAT: {
                    Long last = lastChat.get(player.getUniqueId());
                    if (last == null || now - last >= chatGapMs) {
                        lastChat.put(player.getUniqueId(), now);
                        plugin.getLanguageManager().send(player, "display.format", "%speed%", formatted);
                    }
                    break;
                }

                default:
                    break;
            }
        }
    }

    /** Boss 条进度 = 速度 / 该矿车的生效上限 */
    private double progressOf(double speed, Minecart cart) {
        double max = plugin.getSpeedManager().effectiveLimit(cart);
        if (max <= 0) max = 1;
        return Math.max(0.0D, Math.min(1.0D, speed / max));
    }

    public void removeBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    /** 玩家退出：清理 BossBar 与聊天节流记录（显示方式本身持久化保留） */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBossBar(event.getPlayer());
        lastChat.remove(event.getPlayer().getUniqueId());
    }

    /** 插件关闭时清理：停任务、移除所有 Boss 条、保存数据 */
    public void cleanup() {
        stopTask();
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
        save();
    }
}
