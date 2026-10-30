package com.ximangge.xrail.listener;

import com.ximangge.xrail.XRail;
import com.ximangge.xrail.config.CartType;
import com.ximangge.xrail.gui.SpeedGUI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 潜行 + 左键 + 右键（按顺序）打开单台矿车的速度设置界面，所有玩家可用。
 * 第 1 步：潜行状态下左键（攻击）矿车 → 记录“待设置”状态（并取消伤害，防止打坏矿车）；
 * 第 2 步：4 秒内保持潜行并右键同一台矿车 → 打开 GUI。
 */
public class InteractListener implements Listener {

    private static final long ARM_TIMEOUT_MS = 4000L;

    private static class Armed {
        final UUID cartId;
        final long time;

        Armed(UUID cartId, long time) {
            this.cartId = cartId;
            this.time = time;
        }
    }

    private final XRail plugin;
    private final Map<UUID, Armed> armed = new ConcurrentHashMap<>();

    public InteractListener(XRail plugin) {
        this.plugin = plugin;
    }

    /**
     * 第一步：潜行 + 左键矿车。
     * 用 HIGH + ignoreCancelled=false：即使保护插件（如 WorldGuard）已取消伤害事件，
     * 组合键仍然生效（主城等保护区也能打开 GUI）；非组合键情况不干预事件。
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        if (!(event.getEntity() instanceof Minecart)) return;
        if (!player.isSneaking() || !player.hasPermission("xrail.use")) return;

        event.setCancelled(true); // 组合键期间不损坏矿车
        armed.put(player.getUniqueId(),
                new Armed(event.getEntity().getUniqueId(), System.currentTimeMillis()));
        plugin.getLanguageManager().sendActionbar(player, "gui.armed");
    }

    /** 第二步：右键矿车（普通交互） */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClick(PlayerInteractEntityEvent event) {
        if (handle(player(event), event.getRightClicked(), event.getHand())) {
            event.setCancelled(true); // 阻止直接乘坐矿车
        }
    }

    /** 第二步：右键矿车（带位置的交互，避免重复触发漏判） */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClickAt(PlayerInteractAtEntityEvent event) {
        if (handle(player(event), event.getRightClicked(), event.getHand())) {
            event.setCancelled(true);
        }
    }

    private Player player(PlayerInteractEntityEvent event) {
        return event.getPlayer();
    }

    private boolean handle(Player player, Entity clicked, EquipmentSlot hand) {
        if (hand != EquipmentSlot.HAND) return false;
        if (!(clicked instanceof Minecart)) return false;

        Armed arm = armed.get(player.getUniqueId());
        if (arm == null) return false;
        if (!arm.cartId.equals(clicked.getUniqueId())) return false; // 点错矿车：不消费，仍可右键目标车
        armed.remove(player.getUniqueId());
        if (!player.isSneaking() || !player.hasPermission("xrail.use")) return false;
        if (System.currentTimeMillis() - arm.time > ARM_TIMEOUT_MS) {
            plugin.getLanguageManager().sendActionbar(player, "gui.expired");
            return false;
        }

        CartType type = CartType.fromEntity((Minecart) clicked);
        if (type == null || type.isCommandCart()) {
            plugin.getLanguageManager().send(player, "gui.command-cart");
            return true;
        }
        if (!plugin.getConfigManager().getSetting(type).enabled) {
            plugin.getLanguageManager().send(player, "gui.type-vanilla");
            return true;
        }

        new SpeedGUI(plugin, (Minecart) clicked).open(player);
        plugin.getLanguageManager().send(player, "gui.opened",
                "%type%", plugin.getLanguageManager().format(type.langKey()));
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        armed.remove(event.getPlayer().getUniqueId());
    }
}
