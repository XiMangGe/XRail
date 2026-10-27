package com.ximangge.xrail.config;

import org.bukkit.entity.Minecart;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 矿车类型枚举。
 * 运行时通过实体类型名称匹配，兼容 1.20 的 COMMAND_MINECART
 * 与 1.21+ 的 COMMAND_BLOCK_MINECART 命名差异。
 */
public enum CartType {

    MINECART("MINECART", "cart", "minecart", "普通矿车", "矿车", "普通"),
    CHEST_MINECART("CHEST_MINECART", "chest", "chestcart", "箱子矿车", "箱子"),
    FURNACE_MINECART("FURNACE_MINECART", "furnace", "furnacecart", "powered", "动力矿车", "熔炉矿车", "熔炉"),
    HOPPER_MINECART("HOPPER_MINECART", "hopper", "hoppercart", "漏斗矿车", "漏斗"),
    TNT_MINECART("TNT_MINECART", "tnt", "tntcart", "tnt矿车", "爆炸矿车"),
    SPAWNER_MINECART("SPAWNER_MINECART", "spawner", "spawnercart", "刷怪笼矿车", "刷怪笼"),
    COMMAND_MINECART("COMMAND_MINECART", "command", "commandblock", "commandcart", "命令方块矿车", "命令矿车", "命令");

    private final String key;
    private final String[] aliases;

    CartType(String key, String... aliases) {
        this.key = key;
        this.aliases = aliases;
    }

    public String getKey() {
        return key;
    }

    /** 语言文件中的类型名称 key，例如 type.hopper_minecart */
    public String langKey() {
        return "type." + key.toLowerCase(Locale.ROOT);
    }

    public boolean isCommandCart() {
        return this == COMMAND_MINECART;
    }

    /** 运行时实体类型名 → 矿车类型 */
    public static CartType fromEntityName(String entityTypeName) {
        if (entityTypeName == null) return null;
        switch (entityTypeName.toUpperCase(Locale.ROOT)) {
            case "MINECART":            return MINECART;
            case "CHEST_MINECART":      return CHEST_MINECART;
            case "FURNACE_MINECART":    return FURNACE_MINECART;
            case "HOPPER_MINECART":     return HOPPER_MINECART;
            case "TNT_MINECART":        return TNT_MINECART;
            case "SPAWNER_MINECART":    return SPAWNER_MINECART;
            case "COMMAND_MINECART":
            case "COMMAND_BLOCK_MINECART": return COMMAND_MINECART;
            default: return null;
        }
    }

    public static CartType fromEntity(Minecart cart) {
        return fromEntityName(cart.getType().name());
    }

    /** 命令/Tab 补全输入 → 矿车类型 */
    public static CartType fromInput(String input) {
        if (input == null || input.isEmpty()) return null;
        String norm = normalize(input);
        for (CartType t : values()) {
            if (normalize(t.key).equals(norm)) return t;
            for (String a : t.aliases) {
                if (normalize(a).equals(norm)) return t;
            }
        }
        return null;
    }

    private static String normalize(String s) {
        return s.trim().toUpperCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
    }

    /** Tab 补全建议（不含命令矿车） */
    public static List<String> inputSuggestions() {
        List<String> list = new ArrayList<>();
        for (CartType t : values()) {
            if (t.isCommandCart()) continue;
            list.add(t.key.toLowerCase(Locale.ROOT));
        }
        return list;
    }
}
