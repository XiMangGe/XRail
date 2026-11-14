package com.ximangge.xrail.boost;

import com.ximangge.xrail.XRail;
import com.ximangge.xrail.config.CartType;
import com.ximangge.xrail.config.ConfigManager;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

/**
 * 助推：有乘客的矿车沿行进方向自动加速到该矿车的上限（像无限充能铁轨），
 * 并把重力造成的超速硬性压回目标速度；坡道（上/下坡铁轨）一律限速原版 8 格/秒，
 * 且在进坡前提前减速（高速矿车进坡瞬间会脱轨/飞出，事后补救来不及）。
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

    /** 原版矿车最大速度：0.4 格/tick = 8 格/秒（坡道统一压回该速度） */
    private static final double VANILLA_BLOCKS_PER_TICK = 0.4D;

    private final XRail plugin;

    public BoostManager(XRail plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Minecart)) return;
        Minecart cart = (Minecart) event.getVehicle();
        if (cart.isEmpty() || railAt(cart) == null) return; // 只助推轨道上有乘客的矿车

        CartType type = CartType.fromEntity(cart);
        if (type == null || type.isCommandCart()) return;
        ConfigManager config = plugin.getConfigManager();
        ConfigManager.CartSetting setting = config.getSetting(type);
        if (!setting.enabled || !config.isAssistEnabled()) return;

        Vector velocity = cart.getVelocity();
        double speed = velocity.length(); // 格/tick
        if (speed < MIN_MOVE_SPEED) return;

        double cap = plugin.getSpeedManager().effectiveLimit(cart) / 20.0D;
        // 坡道（上/下坡铁轨）一律把速度压回原版 8 格/秒：下坡"飞下去"、高速冲坡都会被限制住。
        // 必须"提前减速"：高速矿车在进坡的那一 tick 就会脱轨/飞出，
        // 事后再压速来不及，所以沿行进方向向前扫描，发现前方有坡道就先把速度压下来。
        boolean slope = onSlope(cart) || slopeAhead(cart, velocity);
        double target = slope ? Math.min(cap, VANILLA_BLOCKS_PER_TICK) : cap;
        if (speed > target) {
            // 重力分量不受 setMaxSpeed 管（下坡俯冲），硬性压回目标速度
            cart.setVelocity(velocity.normalize().multiply(target));
        } else {
            // 沿当前方向助推：每 tick 增加 acceleration 格/秒²（1 格/秒² = 1/400 格/tick²）
            double step = config.getAssistAcceleration() / 400.0D;
            double newSpeed = Math.min(target, speed + step);
            cart.setVelocity(velocity.normalize().multiply(newSpeed));
        }
    }

    /** 矿车当前所在方块的铁轨数据，不在铁轨上返回 null（paper-api 1.20.4 没有 isOnRails） */
    private Rail railAt(Minecart cart) {
        return cart.getLocation().getBlock().getBlockData() instanceof Rail rail ? rail : null;
    }

    /** 矿车所在铁轨是否为坡道（上/下坡，ASCENDING_* 形状） */
    private boolean onSlope(Minecart cart) {
        Rail rail = railAt(cart);
        return rail != null && isAscending(rail);
    }

    /**
     * 行进方向前方是否快到坡道了。
     * 扫描距离 = 本 tick 位移距离 + 1 格，保证高速（每 tick 跨多格）也不会一头扎进坡道。
     */
    private boolean slopeAhead(Minecart cart, Vector velocity) {
        Vector dir = new Vector(velocity.getX(), 0, velocity.getZ());
        double len = dir.length();
        if (len < 1.0E-6D) return false;
        dir.multiply(1.0D / len);
        int look = (int) Math.ceil(velocity.length()) + 1;
        org.bukkit.Location base = cart.getLocation();
        for (int i = 1; i <= look; i++) {
            org.bukkit.block.Block block = base.clone().add(dir.clone().multiply(i)).getBlock();
            if (block.getBlockData() instanceof Rail rail && isAscending(rail)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAscending(Rail rail) {
        return rail.getShape().name().startsWith("ASCENDING");
    }
}
