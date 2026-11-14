package com.ximangge.xrail.boost;

import com.ximangge.xrail.XRail;
import com.ximangge.xrail.config.CartType;
import com.ximangge.xrail.config.ConfigManager;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

/**
 * 助推：有乘客的矿车沿行进方向自动加速到该矿车的上限（像无限充能铁轨），
 * 并把下坡等场景中重力造成的超速硬性压回上限。
 *
 * 背景：Bukkit 的 setMaxSpeed 只约束沿轨道的水平速度，
 * 下坡时重力加速度不受上限约束；且原版移动是"每 tick 一步"，
 * 速度超过约 1 格/tick 后在急弯/陡坡处会撞上方块边界弹回——这是引擎本身的限制，
 * 助推能解决爬坡和提速，但无法突破引擎的高速弯道/陡坡上限。
 *
 * 实现方式：VehicleMoveEvent 每次移动调整下一 tick 的速度向量，
 * 配置实时读取，/xrail reload 立即生效。
 */
public class BoostManager implements Listener {

    /** 低于该速度（格/tick）视为静止，不自动起步（推一下或充能铁轨启动） */
    private static final double MIN_MOVE_SPEED = 1.0E-4D;

    private final XRail plugin;

    public BoostManager(XRail plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Minecart)) return;
        Minecart cart = (Minecart) event.getVehicle();
        if (cart.isEmpty() || !onRails(cart)) return; // 只助推轨道上有乘客的矿车

        CartType type = CartType.fromEntity(cart);
        if (type == null || type.isCommandCart()) return;
        ConfigManager config = plugin.getConfigManager();
        ConfigManager.CartSetting setting = config.getSetting(type);
        if (!setting.enabled || !config.isAssistEnabled()) return;

        Vector velocity = cart.getVelocity();
        double speed = velocity.length(); // 格/tick
        if (speed < MIN_MOVE_SPEED) return;

        double cap = plugin.getSpeedManager().effectiveLimit(cart) / 20.0D;
        if (speed > cap) {
            // 重力分量不受 setMaxSpeed 管（下坡俯冲），硬性压回上限
            cart.setVelocity(velocity.normalize().multiply(cap));
        } else {
            // 沿当前方向助推：每 tick 增加 acceleration 格/秒²（1 格/秒² = 1/400 格/tick²）
            double step = config.getAssistAcceleration() / 400.0D;
            double newSpeed = Math.min(cap, speed + step);
            cart.setVelocity(velocity.normalize().multiply(newSpeed));
        }
    }

    /** 矿车当前所在方块是否为铁轨（paper-api 1.20.4 没有 isOnRails，用方块类型判断） */
    private boolean onRails(Minecart cart) {
        return cart.getLocation().getBlock().getType().name().endsWith("RAIL");
    }
}
