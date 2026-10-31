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

    /** 上限的硬性上界，防止配置出离谱值破坏游戏物理（速度单位：格/秒） */
    public static final int HARD_CAP = 500;

    private String language;
    private boolean displayEnabled;
    private String defaultMethod;
    private int updateInterval;
    private int chatInterval;
    private int defaultLimit; // 兼容旧配置 limit.default 的兜底值，新配置不再使用
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
        defaultLimit = clampBound(yaml.getInt("limit.default", 25), 25);

        carts.clear();
        for (CartType type : CartType.values()) {
            if (type.isCommandCart()) continue; // 命令方块矿车始终原版
            String path = "carts." + type.getKey();
            boolean enabled = yaml.getBoolean(path + ".enabled", true);
            int limit = yaml.getInt(path + ".limit", defaultLimit);
            carts.put(type, new CartSetting(enabled, clamp(limit)));
        }
    }

    /**
     * 把矿车类型配置与语言写回 config.yml。
     * 保存前先重读磁盘：只覆盖插件自己管理的键（language / carts.*），
     * 避免运行期手动编辑过的 display、limit 等配置被内存快照覆盖丢失。
     */
    public void save() {
        try {
            YamlConfiguration disk = file.isFile()
                    ? YamlConfiguration.loadConfiguration(file)
                    : yaml; // 文件尚未生成时用内存配置作底稿
            disk.set("language", language);
            for (Map.Entry<CartType, CartSetting> e : carts.entrySet()) {
                String path = "carts." + e.getKey().getKey();
                disk.set(path + ".enabled", e.getValue().enabled);
                disk.set(path + ".limit", e.getValue().limit);
            }
            disk.save(file);
            yaml = disk;
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

    /** 速度钳制到 [1, HARD_CAP] */
    public int clamp(int v) {
        return Math.max(1, Math.min(v, HARD_CAP));
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
        s.limit = clamp(limit);
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
}
