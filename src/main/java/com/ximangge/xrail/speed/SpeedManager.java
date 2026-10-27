package com.ximangge.xrail.speed;

import com.ximangge.xrail.XRail;
import com.ximangge.xrail.config.CartType;
import com.ximangge.xrail.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.persistence.PersistentDataType;

/**
 * 矿车速度管理。
 * Bukkit 的 setMaxSpeed 单位是“格/tick”（原版 0.4 = 8 格/秒），
 * 本插件对外的单位统一为“格/秒”，写入时除以 20。
 *
 * 上限规则：普通矿车（MINECART）最高 limit.max-cart（默认 75），
 * 其他类型最高 limit.max-other（默认 25），命令方块矿车始终原版。
 * 玩家可通过 GUI 对单台矿车设置自定义上限（存入矿车持久化数据，随存档保存）。
 */
public class SpeedManager {

    /** 原版矿车最大速度：0.4 格/tick = 8 格/秒 */
    public static final double VANILLA_BLOCKS_PER_TICK = 0.4D;
    public static final int VANILLA_BLOCKS_PER_SECOND = 8;

    private final XRail plugin;
    private final NamespacedKey overrideKey;

    public SpeedManager(XRail plugin) {
        this.plugin = plugin;
        this.overrideKey = new NamespacedKey(plugin, "cart_speed_limit");
    }

    public static double toBlocksPerTick(double blocksPerSecond) {
        return blocksPerSecond / 20.0D;
    }

    /** 读取单台矿车的自定义上限（未设置返回 null） */
    public Integer getOverride(Minecart cart) {
        return cart.getPersistentDataContainer().get(overrideKey, PersistentDataType.INTEGER);
    }

    /** 写入/清除单台矿车的自定义上限（null 清除） */
    public void setOverride(Minecart cart, Integer limit) {
        if (limit == null) {
            cart.getPersistentDataContainer().remove(overrideKey);
        } else {
            cart.getPersistentDataContainer().set(overrideKey, PersistentDataType.INTEGER, limit);
        }
    }

    /** 单台矿车最终生效的速度上限（格/秒） */
    public int effectiveLimit(Minecart cart) {
        CartType type = CartType.fromEntity(cart);
        if (type == null || type.isCommandCart()) return VANILLA_BLOCKS_PER_SECOND;
        ConfigManager.CartSetting setting = plugin.getConfigManager().getSetting(type);
        if (!setting.enabled) return VANILLA_BLOCKS_PER_SECOND;
        Integer override = getOverride(cart);
        return override != null
                ? plugin.getConfigManager().clamp(type, override)
                : setting.limit;
    }

    /** 按配置对单台矿车应用速度上限（命令方块矿车不处理） */
    public void apply(Minecart cart) {
        CartType type = CartType.fromEntity(cart);
        if (type == null || type.isCommandCart()) return;
        if (!plugin.getConfigManager().getSetting(type).enabled) {
            // 保持原版行为
            cart.setMaxSpeed(VANILLA_BLOCKS_PER_TICK);
            return;
        }
        cart.setMaxSpeed(toBlocksPerTick(effectiveLimit(cart)));
    }

    /** 对某一类型的所有已加载矿车立即生效（命令修改后调用） */
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
     * （单台矿车的自定义上限存在 PDC 中，会随存档持久化，扫描时自动恢复。）
     */
    public void startSweepTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::applyAllLoaded, 200L, 200L);
    }
}
