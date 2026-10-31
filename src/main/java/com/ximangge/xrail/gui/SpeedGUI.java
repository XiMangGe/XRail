package com.ximangge.xrail.gui;

import com.ximangge.xrail.XRail;
import com.ximangge.xrail.config.CartType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 单台矿车的速度设置界面（54 格箱子界面），所有玩家可用。
 * - 第 1 页：钻石，每个 ±5 格/秒（数量 = 当前上限 ÷ 5）
 * - 第 2 页：红石粉，每个 ±1 格/秒（数量 = 当前上限 ÷ 5 的余数）
 * - 右键加、左键减；箭头翻页，屏障关闭
 * 修改只作用于这一台矿车（存入其持久化数据，随存档保存），
 * 且不能超过该类型在 config.yml carts 中配置的速度上限。
 */
public class SpeedGUI implements InventoryHolder {

    public static final int SIZE = 54;

    /** 同一台矿车当前打开的所有界面（多人同开时互相联动刷新） */
    private static final Map<UUID, List<SpeedGUI>> OPEN = new ConcurrentHashMap<>();

    private static final int SLOT_ITEM = 22;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_RESET = 40;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 53;

    private final XRail plugin;
    private final Minecart cart;
    private final CartType type;
    private final Inventory inventory;
    private int page = 0;

    public SpeedGUI(XRail plugin, Minecart cart) {
        this.plugin = plugin;
        this.cart = cart;
        this.type = CartType.fromEntity(cart);
        String title = plugin.getLanguageManager().format("gui.title", "%type%", typeDisplay());
        this.inventory = Bukkit.createInventory(this, SIZE, title);
        OPEN.computeIfAbsent(cart.getUniqueId(), k -> new CopyOnWriteArrayList<>()).add(this);
        refresh();
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public CartType getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String typeDisplay() {
        return plugin.getLanguageManager().format(type.langKey());
    }

    /** 这台矿车当前的生效上限 */
    private int currentLimit() {
        return plugin.getSpeedManager().effectiveLimit(cart);
    }

    /** 这台矿车所属类型在配置中允许的最高速度 */
    private int cap() {
        return plugin.getConfigManager().getSetting(type).limit;
    }

    /** 关闭界面时从共享注册表移除（由 InventoryCloseEvent 调用） */
    public void unregister() {
        List<SpeedGUI> list = OPEN.get(cart.getUniqueId());
        if (list != null) {
            list.remove(this);
            if (list.isEmpty()) OPEN.remove(cart.getUniqueId());
        }
    }

    /** 同一台矿车的其他玩家界面同步刷新 */
    private void refreshOthers() {
        List<SpeedGUI> list = OPEN.get(cart.getUniqueId());
        if (list == null) return;
        for (SpeedGUI gui : list) {
            if (gui != this) gui.refresh();
        }
    }

    public void setPage(int page) {
        this.page = Math.max(0, Math.min(1, page));
        refresh();
    }

    /** 重新绘制界面内容 */
    public void refresh() {
        inventory.clear();
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        int limit = currentLimit();
        int step = page == 0 ? 5 : 1;
        Material material = page == 0 ? Material.DIAMOND : Material.REDSTONE;
        int count = page == 0 ? limit / 5 : limit % 5;

        List<String> itemLore = new ArrayList<>();
        itemLore.add(plugin.getLanguageManager().format("gui.item-lore", "%step%", String.valueOf(step)));
        ItemStack item;
        if (count == 0) {
            // 本页数量为 0：灰染料空态，如实显示 0（右键仍可添加）
            item = named(Material.GRAY_DYE,
                    plugin.getLanguageManager().format("gui.item-empty",
                            "%step%", String.valueOf(step)),
                    itemLore);
            item.setAmount(1);
        } else {
            item = named(material,
                    plugin.getLanguageManager().format("gui.item-name",
                            "%step%", String.valueOf(step),
                            "%count%", String.valueOf(count)),
                    itemLore);
            item.setAmount(Math.min(64, count));
        }
        inventory.setItem(SLOT_ITEM, item);

        List<String> infoLore = new ArrayList<>();
        infoLore.add(plugin.getLanguageManager().format("gui.info-limit", "%speed%", String.valueOf(limit)));
        infoLore.add(plugin.getLanguageManager().format("gui.info-max", "%max%", String.valueOf(cap())));
        infoLore.add(plugin.getLanguageManager().format("gui.info-help"));
        inventory.setItem(SLOT_INFO, named(Material.BOOK,
                plugin.getLanguageManager().format("gui.info-name", "%type%", typeDisplay()), infoLore));

        inventory.setItem(SLOT_PREV, named(page > 0 ? Material.ARROW : Material.GRAY_DYE,
                plugin.getLanguageManager().format("gui.prev"), null));
        inventory.setItem(SLOT_NEXT, named(page < 1 ? Material.ARROW : Material.GRAY_DYE,
                plugin.getLanguageManager().format("gui.next"), null));
        inventory.setItem(SLOT_CLOSE, named(Material.BARRIER,
                plugin.getLanguageManager().format("gui.close"), null));

        // 重置按钮：有单车覆写时可恢复跟随类型默认
        boolean hasOverride = plugin.getSpeedManager().getOverride(cart) != null;
        inventory.setItem(SLOT_RESET, named(hasOverride ? Material.LIME_DYE : Material.GRAY_DYE,
                plugin.getLanguageManager().format(hasOverride ? "gui.reset" : "gui.reset-none"), null));
    }

    /** 下一 tick 关闭界面（避免在点击事件中同步关闭导致 desync） */
    private void closeLater(Player player) {
        Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
    }

    /** 处理界面内的点击（调用方已取消事件） */
    public void handleClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case SLOT_ITEM: {
                // 只响应左键（减）与右键（加），中键/数字键不调整
                if (!click.isLeftClick() && !click.isRightClick()) break;
                int step = page == 0 ? 5 : 1;
                adjust(player, click.isRightClick() ? step : -step);
                break;
            }
            case SLOT_PREV:
                if (page > 0) setPage(page - 1);
                break;
            case SLOT_NEXT:
                if (page < 1) setPage(page + 1);
                break;
            case SLOT_CLOSE:
                closeLater(player);
                break;
            case SLOT_RESET:
                // 与调速物品一致：只响应左键/右键，防中键/数字键穿透
                if (click.isLeftClick() || click.isRightClick()) {
                    if (plugin.getSpeedManager().getOverride(cart) != null) {
                        plugin.getSpeedManager().setOverride(cart, null);
                        plugin.getSpeedManager().apply(cart);
                        refresh();
                        refreshOthers();
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void adjust(Player player, int delta) {
        if (!plugin.getConfigManager().getSetting(type).enabled) {
            // GUI 打开期间类型被切回原版：拒绝调整，保持状态一致
            plugin.getLanguageManager().send(player, "gui.type-vanilla");
            closeLater(player);
            return;
        }
        int oldLimit = currentLimit();
        int max = cap();
        int newLimit = oldLimit + delta;

        if (newLimit > max) {
            plugin.getLanguageManager().send(player, "gui.max", "%max%", String.valueOf(max));
            newLimit = max;
        } else if (newLimit < 1) {
            plugin.getLanguageManager().send(player, "gui.min");
            newLimit = 1;
        }
        if (newLimit == oldLimit) return;

        plugin.getSpeedManager().setOverride(cart, newLimit);
        plugin.getSpeedManager().apply(cart);
        refresh();
        refreshOthers();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
