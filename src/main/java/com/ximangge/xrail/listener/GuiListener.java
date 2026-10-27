package com.ximangge.xrail.listener;

import com.ximangge.xrail.XRail;
import com.ximangge.xrail.gui.SpeedGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * GUI 点击/拖拽保护与交互分发。
 */
public class GuiListener implements Listener {

    private final XRail plugin;

    public GuiListener(XRail plugin) {
        this.plugin = plugin;
    }

    private SpeedGUI guiOf(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof SpeedGUI) {
            return (SpeedGUI) top.getHolder();
        }
        return null;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        SpeedGUI gui = guiOf(event);
        if (gui == null) return;

        event.setCancelled(true); // 界面内所有物品均不可取走
        if (!(event.getWhoClicked() instanceof Player)) return;

        Inventory top = event.getView().getTopInventory();
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(top)) return; // 点击玩家自己的背包：忽略

        gui.handleClick((Player) event.getWhoClicked(), event.getSlot(), event.getClick());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof SpeedGUI) {
            event.setCancelled(true);
        }
    }
}
