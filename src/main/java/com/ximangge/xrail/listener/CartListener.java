package com.ximangge.xrail.listener;

import com.ximangge.xrail.XRail;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

/**
 * 在矿车生成、区块实体加载时立即应用速度限制。
 * 其余漏网场景由 SpeedManager 的周期扫描兜底。
 */
public class CartListener implements Listener {

    private final XRail plugin;

    public CartListener(XRail plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        if (event.getVehicle() instanceof Minecart) {
            plugin.getSpeedManager().apply((Minecart) event.getVehicle());
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Minecart) {
                plugin.getSpeedManager().apply((Minecart) entity);
            }
        }
    }
}
