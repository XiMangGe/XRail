package com.ximangge.xrail.language;

import com.ximangge.xrail.XRail;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 语言管理：
 * - 首次运行时把内置的 zh_cn / en_us 语言文件释放到 plugins/XRail/lang/
 * - 支持玩家自行放入其他语言文件（如 custom.yml），用 /xrail language custom 切换
 * - 读取失败时回退到 en_us
 */
public class LanguageManager {

    private final XRail plugin;
    private final File langDir;

    private YamlConfiguration messages = new YamlConfiguration();
    private YamlConfiguration fallback = new YamlConfiguration();
    private String prefix = "";

    public LanguageManager(XRail plugin) {
        this.plugin = plugin;
        this.langDir = new File(plugin.getDataFolder(), "lang");
    }

    /** 首次运行生成语言文件夹和默认语言文件 */
    public void ensureDefaults() {
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        saveIfAbsent("lang/zh_cn.yml");
        saveIfAbsent("lang/en_us.yml");
    }

    private void saveIfAbsent(String resource) {
        File target = new File(plugin.getDataFolder(), resource);
        if (!target.exists()) {
            try {
                plugin.saveResource(resource, false);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Missing bundled resource: " + resource);
            }
        }
    }

    /** 语言文件（lang/<code>.yml）是否存在 */
    public boolean exists(String code) {
        if (code == null || code.isEmpty()) return false;
        return new File(langDir, code + ".yml").isFile();
    }

    /** 加载指定语言（不存在时回退 en_us 并告警） */
    public void load(String code) {
        File baseFile = new File(langDir, code + ".yml");
        if (!baseFile.isFile()) {
            if (!"en_us".equals(code)) {
                plugin.getLogger().warning("Language file lang/" + code + ".yml not found, falling back to en_us.");
            }
            baseFile = new File(langDir, "en_us.yml");
        }
        messages = baseFile.isFile() ? YamlConfiguration.loadConfiguration(baseFile) : new YamlConfiguration();
        File fb = new File(langDir, "en_us.yml");
        fallback = fb.isFile() ? YamlConfiguration.loadConfiguration(fb) : new YamlConfiguration();
        prefix = color(raw("prefix"));
    }

    /** 取原始文本（未着色、未替换占位符），缺失时回退 en_us，再缺失返回 key 本身 */
    public String raw(String key) {
        String v = messages.getString(key);
        if (v == null) v = fallback.getString(key);
        return v != null ? v : key;
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String apply(String text, String... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], placeholders[i + 1]);
        }
        return color(text);
    }

    /** 取消息并替换占位符（成对传入，如 "%speed%", "12.500"） */
    public String format(String key, String... placeholders) {
        return apply(raw(key), placeholders);
    }

    /** 取消息列表（如 help），替换占位符 */
    public List<String> lines(String key, String... placeholders) {
        List<String> list = messages.getStringList(key);
        if (list.isEmpty()) list = fallback.getStringList(key);
        List<String> out = new ArrayList<>();
        for (String line : list) {
            out.add(apply(line, placeholders));
        }
        return out;
    }

    /** 发送带插件前缀的消息 */
    public void send(CommandSender to, String key, String... placeholders) {
        to.sendMessage(prefix + format(key, placeholders));
    }

    /** 发送不带前缀的消息（用于 help / status 等多行内容） */
    public void sendRaw(CommandSender to, String key, String... placeholders) {
        to.sendMessage(format(key, placeholders));
    }

    /** 发送原始文本（不带 key 查找） */
    public void sendText(CommandSender to, String text) {
        to.sendMessage(color(text));
    }

    /** 发送 Action Bar 提示 */
    public void sendActionbar(Player player, String key, String... placeholders) {
        sendActionbarRaw(player, format(key, placeholders));
    }

    /** 发送 Action Bar（已格式化文本，供需要去重的调用方使用） */
    public void sendActionbarRaw(Player player, String text) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(text);
        player.sendActionBar(component);
    }

    public String getPrefix() {
        return prefix;
    }
}
