package com.ximangge.xrail.speed;

import com.ximangge.xrail.XRail;
import com.ximangge.xrail.config.CartType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;

/**
 * 矿车速度管理。
 * Bukkit 的 setMaxSpeed 单位是“格/tick”（原版 0.4 = 8 格/秒），
 * 本插件对外的单位统一为“格/秒”，写入时除以 20。
 */
public class SpeedManager {

    /** 原版矿车最大速度：0.4 格/tick = 8 格/秒 */
    public static final double VANILLA_BLOCKS_PER_TICK = 0.4D;

    private final XRail plugin;

    public SpeedManager(XRail plugin) {
        this.plugin = plugin;
    }

    public static double toBlocksPerTick(double blocksPerSecond) {
        return blocksPerSecond / 20.0D;
    }

    /** 按配置对单台矿车应用速度上限（命令方块矿车不处理） */
    public void apply(Minecart cart) {
        CartType type = CartType.fromEntity(cart);
        if (type == null || type.isCommandCart()) return;
        boolean enabled = plugin.getConfigManager().getSetting(type).enabled;
        int limit = plugin.getConfigManager().getSetting(type).limit;
        if (enabled) {
            cart.setMaxSpeed(toBlocksPerTick(limit));
        } else {
            // 保持原版行为
            cart.setMaxSpeed(VANILLA_BLOCKS_PER_TICK);
        }
    }

    /** 对某一类型的所有已加载矿车立即生效（GUI/命令修改后调用） */
    public void applyType(CartType type) {
        if (type == null || type.isCommandCart()) return;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Minecart) {
                    Minecart cart = (Minecart) entity;
                    if (type == CartType.fromEntity(cart)) {
                        apply(cart);
                    }
                }
            }
        }
    }

    /** 对所有已加载矿车应用配置（启动/重载时调用） */
    public void applyAllLoaded() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Minecart) {
                    apply((Minecart) entity);
                }
            }
        }
    }

    /**
     * 兜底扫描任务：矿车的 maxSpeed 不随存档持久化，
     * 每 10 秒扫描一次已加载矿车，补齐区块加载等遗漏场景。
     */
    public void startSweepTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::applyAllLoaded, 200L, 200L);
    }
}
