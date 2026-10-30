package com.ximangge.xrail.config;

import com.ximangge.xrail.XRail;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * 配置管理：config.yml 的读写。
 * 记录插件语言、显示栏开关、以及每种矿车是否修改速度/速度上限。
 */
public class ConfigManager {

    /** 单个矿车类型的速度配置 */
    public static class CartSetting {
        public boolean enabled;
        public int limit;

        public CartSetting(boolean enabled, int limit) {
            this.enabled = enabled;
            this.limit = limit;
        }
    }

    private final XRail plugin;
    private final File file;
    private YamlConfiguration yaml;

    /** 上限的硬性上界，防止配置出离谱值破坏游戏物理 */
    private static final int HARD_CAP = 500;

    private String language;
    private boolean displayEnabled;
    private String defaultMethod;
    private int updateInterval;
    private int chatInterval;
    private int maxCart;      // 普通矿车最高上限
    private int maxOther;     // 其他类型矿车最高上限
    private int defaultLimit;
    private final Map<CartType, CartSetting> carts = new EnumMap<>(CartType.class);

    public ConfigManager(XRail plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "config.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);

        language = yaml.getString("language", "zh_cn");
        displayEnabled = yaml.getBoolean("display.enabled", true);
        defaultMethod = yaml.getString("display.default-method", "ACTIONBAR").toUpperCase(Locale.ROOT);
        updateInterval = Math.max(1, yaml.getInt("display.update-interval", 2));
        chatInterval = Math.max(1, yaml.getInt("display.chat-interval", 20));
        maxCart = yaml.getInt("limit.max-cart", 75);
        maxOther = yaml.getInt("limit.max-other", 25);
        defaultLimit = yaml.getInt("limit.default", 25);
        maxCart = clampBound(maxCart, 75);
        maxOther = clampBound(maxOther, 25);
        defaultLimit = clampBound(defaultLimit, 25);

        carts.clear();
        for (CartType type : CartType.values()) {
            if (type.isCommandCart()) continue; // 命令方块矿车始终原版
            String path = "carts." + type.getKey();
            boolean enabled = yaml.getBoolean(path + ".enabled", true);
            int limit = yaml.getInt(path + ".limit", defaultLimit);
            carts.put(type, new CartSetting(enabled, clamp(type, limit)));
        }
    }

    /** 把矿车类型配置写回 config.yml */
    public void save() {
        try {
            for (Map.Entry<CartType, CartSetting> e : carts.entrySet()) {
                String path = "carts." + e.getKey().getKey();
                yaml.set(path + ".enabled", e.getValue().enabled);
                yaml.set(path + ".limit", e.getValue().limit);
            }
            yaml.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save config.yml: " + ex.getMessage());
        }
    }

    /** 上限值钳制到 [1, HARD_CAP]，非法值回退默认并告警 */
    private int clampBound(int v, int def) {
        if (v < 1) return def;
        if (v > HARD_CAP) {
            plugin.getLogger().warning("Speed limit " + v + " exceeds hard cap " + HARD_CAP
                    + " blocks/s, clamped. Please fix config.yml.");
            return HARD_CAP;
        }
        return v;
    }

    /** 某类矿车的最高上限：普通矿车 max-cart，其他类型 max-other */
    public int getTypeCap(CartType type) {
        return type == CartType.MINECART ? maxCart : maxOther;
    }

    /** 速度钳制到 [1, 该类型上限] */
    public int clamp(CartType type, int v) {
        return Math.max(1, Math.min(v, getTypeCap(type)));
    }

    public CartSetting getSetting(CartType type) {
        if (type.isCommandCart()) return new CartSetting(false, 8);
        CartSetting s = carts.get(type);
        return s != null ? s : new CartSetting(false, defaultLimit);
    }

    /** 设置某类矿车的速度上限（同时启用修改） */
    public void setLimit(CartType type, int limit) {
        if (type.isCommandCart()) return;
        CartSetting s = getSetting(type);
        s.enabled = true;
        s.limit = clamp(type, limit);
        carts.put(type, s);
        save();
    }

    /** 某类矿车恢复原版（不再修改速度） */
    public void setVanilla(CartType type) {
        if (type.isCommandCart()) return;
        CartSetting s = getSetting(type);
        s.enabled = false;
        carts.put(type, s);
        save();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String lang) {
        this.language = lang;
        yaml.set("language", lang);
        save();
    }

    public boolean isDisplayEnabled() {
        return displayEnabled;
    }

    public String getDefaultMethod() {
        return defaultMethod;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public int getChatInterval() {
        return chatInterval;
    }

    public int getMaxCart() {
        return maxCart;
    }

    public int getMaxOther() {
        return maxOther;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }
}
