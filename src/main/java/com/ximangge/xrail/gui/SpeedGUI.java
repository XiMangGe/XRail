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

/**
 * 单台矿车的速度设置界面（54 格箱子界面），所有玩家可用。
 * - 第 1 页：钻石，每个 ±5 格/秒（数量 = 当前上限 ÷ 5）
 * - 第 2 页：红石粉，每个 ±1 格/秒（数量 = 当前上限 ÷ 5 的余数）
 * - 右键加、左键减；箭头翻页，屏障关闭
 * 修改只作用于这一台矿车（存入其持久化数据，随存档保存），
 * 且不能超过该类型的最高上限：普通矿车 75，其他类型 25。
 */
public class SpeedGUI implements InventoryHolder {

    public static final int SIZE = 54;
    private static final int SLOT_ITEM = 22;
    private static final int SLOT_INFO = 13;
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

    /** 这台矿车所属类型允许的最高上限 */
    private int cap() {
        return plugin.getConfigManager().getTypeCap(type);
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
        ItemStack item = named(material,
                plugin.getLanguageManager().format("gui.item-name",
                        "%step%", String.valueOf(step),
                        "%count%", String.valueOf(count)),
                itemLore);
        item.setAmount(Math.max(1, Math.min(64, count)));
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
    }

    /** 处理界面内的点击（调用方已取消事件） */
    public void handleClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case SLOT_ITEM: {
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
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    private void adjust(Player player, int delta) {
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
