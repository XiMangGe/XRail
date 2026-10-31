package com.ximangge.xrail.command;

import com.ximangge.xrail.XRail;
import com.ximangge.xrail.config.CartType;
import com.ximangge.xrail.config.ConfigManager;
import com.ximangge.xrail.display.DisplayManager;
import com.ximangge.xrail.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /xrail (别名 /xr) 命令。
 * 子命令：help / status / set / vanilla / display / language / reload
 */
public class XRailCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "help", "status", "set", "vanilla", "display", "language", "reload");
    private static final List<String> DISPLAY_METHODS = Arrays.asList(
            "actionbar", "bossbar", "chat", "off");

    private final XRail plugin;

    public XRailCommand(XRail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help":
                help(sender);
                return true;
            case "status":
                status(sender);
                return true;
            case "set":
                set(sender, args);
                return true;
            case "vanilla":
                vanilla(sender, args);
                return true;
            case "language":
                language(sender, args);
                return true;
            case "display":
                display(sender, args);
                return true;
            case "reload":
                reload(sender);
                return true;
            default:
                lang.send(sender, "unknown-sub");
                return true;
        }
    }

    private void help(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();
        lang.send(sender, "help-header");
        for (String line : lang.lines("help")) {
            sender.sendMessage(line);
        }
    }

    private void status(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();
        lang.send(sender, "status-header");
        for (CartType type : CartType.values()) {
            String state;
            if (type.isCommandCart()) {
                state = lang.format("status-locked");
            } else {
                ConfigManager.CartSetting s = plugin.getConfigManager().getSetting(type);
                state = s.enabled
                        ? lang.format("status-custom", "%speed%", String.valueOf(s.limit))
                        : lang.format("status-vanilla");
            }
            lang.sendRaw(sender, "status-line",
                    "%type%", lang.format(type.langKey()),
                    "%state%", state);
        }
    }

    private void set(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            lang.send(sender, "usage-set");
            return;
        }
        CartType type = CartType.fromInput(args[1]);
        if (type == null || type.isCommandCart()) {
            lang.send(sender, "invalid-type", "%type%", args[1]);
            return;
        }
        int cap = ConfigManager.HARD_CAP;
        int num;
        try {
            num = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            lang.send(sender, "invalid-number", "%input%", args[2], "%max%", String.valueOf(cap));
            return;
        }
        int clamped = plugin.getConfigManager().clamp(num);
        if (clamped != num) {
            lang.send(sender, "speed-clamped", "%value%", String.valueOf(clamped));
        }
        plugin.getConfigManager().setLimit(type, clamped);
        plugin.getSpeedManager().applyType(type);
        lang.send(sender, "set-done",
                "%type%", lang.format(type.langKey()),
                "%speed%", String.valueOf(clamped));
    }

    private void vanilla(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            lang.send(sender, "usage-vanilla");
            return;
        }
        CartType type = CartType.fromInput(args[1]);
        if (type == null || type.isCommandCart()) {
            lang.send(sender, "invalid-type", "%type%", args[1]);
            return;
        }
        plugin.getConfigManager().setVanilla(type);
        plugin.getSpeedManager().applyType(type);
        lang.send(sender, "vanilla-done", "%type%", lang.format(type.langKey()));
    }

    private void language(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            lang.send(sender, "usage-language");
            return;
        }
        String code = args[1].toLowerCase(Locale.ROOT);
        if (!lang.exists(code)) {
            lang.send(sender, "language-unknown", "%code%", code);
            return;
        }
        plugin.getConfigManager().setLanguage(code);
        lang.load(code);
        lang.send(sender, "language-set", "%lang%", code);
    }

    private void display(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();
        DisplayManager display = plugin.getDisplayManager();

        // 无参数：查询自己的显示方式
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                lang.send(sender, "player-only");
                return;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("xrail.display") && !sender.hasPermission("xrail.admin")) {
                lang.send(sender, "no-permission");
                return;
            }
            lang.send(sender, "display-current", "%method%", display.methodOf(player).lower());
            return;
        }

        DisplayManager.Method method = DisplayManager.Method.fromString(args[1]);
        if (method == null) {
            lang.send(sender, "display-unknown-method", "%method%", args[1]);
            return;
        }

        Player target;
        if (args.length >= 3) {
            // 指定其他玩家：仅管理员
            if (!sender.hasPermission("xrail.admin")) {
                lang.send(sender, "no-permission");
                return;
            }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                lang.send(sender, "player-not-found", "%player%", args[2]);
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                lang.send(sender, "player-only");
                return;
            }
            target = (Player) sender;
            if (!target.hasPermission("xrail.display") && !sender.hasPermission("xrail.admin")) {
                lang.send(sender, "no-permission");
                return;
            }
        }

        if (!plugin.getConfigManager().isDisplayEnabled()) {
            lang.send(sender, "display-disabled");
            return;
        }
        display.setMethod(target, method);
        lang.send(sender, "display-set",
                "%method%", method.lower(),
                "%player%", target.getName());
    }

    private void reload(CommandSender sender) {
        if (!requireAdmin(sender)) return;
        plugin.reloadAll();
        plugin.getLanguageManager().send(sender, "reload-done");
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("xrail.admin")) return true;
        plugin.getLanguageManager().send(sender, "no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String prefix = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";

        if (args.length == 1) {
            for (String sub : SUB_COMMANDS) {
                if (permFor(sender, sub) && sub.startsWith(prefix)) out.add(sub);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("set") || sub.equals("vanilla")) {
                if (sender.hasPermission("xrail.admin")) {
                    for (String t : CartType.inputSuggestions()) {
                        if (t.startsWith(prefix)) out.add(t);
                    }
                }
            } else if (sub.equals("language")) {
                if (sender.hasPermission("xrail.admin")) {
                    for (String l : availableLanguages()) {
                        if (l.startsWith(prefix)) out.add(l);
                    }
                }
            } else if (sub.equals("display")) {
                for (String m : DISPLAY_METHODS) {
                    if (m.startsWith(prefix)) out.add(m);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("display")) {
            if (sender.hasPermission("xrail.admin")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(p.getName());
                }
            }
        }
        return out;
    }

    private boolean permFor(CommandSender sender, String sub) {
        switch (sub) {
            case "set":
            case "vanilla":
            case "reload":
            case "language":
                return sender.hasPermission("xrail.admin");
            case "display":
                return sender.hasPermission("xrail.display") || sender.hasPermission("xrail.admin");
            default:
                return sender.hasPermission("xrail.use");
        }
    }

    private List<String> availableLanguages() {
        List<String> list = new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), "lang");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                list.add(f.getName().substring(0, f.getName().length() - 4));
            }
        }
        return list;
    }
}
