# XRail

Minecraft Paper / Purpur 1.20+ 矿车速度控制插件。

按矿车类型精细化控制速度上限（最高 75 格/秒），支持箱子界面设置、实时速度显示与多语言。

## 功能

- **按类型限速**：普通 / 箱子 / 动力 / 漏斗 / TNT / 刷怪笼矿车均可单独设置速度上限（1 ~ 75 格/秒，整数）；也可让某类矿车保持原版
- **命令方块矿车例外**：命令方块矿车（Command Minecart）始终不受影响
- **GUI 设置**：管理员 `潜行 + 左键 + 右键`（按顺序点击）矿车打开箱子界面
  - 第 1 页：钻石，每个 ±5 格/秒（个数 = 当前上限 ÷ 5）
  - 第 2 页：红石粉，每个 ±1 格/秒（个数 = 余数）
  - 右键加、左键减，箭头翻页，屏障关闭；修改即时生效并写回配置
- **实时速度显示**：乘坐矿车时以 3 位小数实时显示速度（格/秒），方式自选：
  - `actionbar` —— 物品栏上方动作条（默认）
  - `bossbar` —— 屏幕上方 Boss 血条（进度随速度/上限变化）
  - `chat` —— 聊天栏定期播报
  - `off` —— 关闭
- **多语言**：首次运行生成 `lang/` 文件夹，内置 `zh_cn` / `en_us`，也可自行添加语言文件
- **命令反馈**：所有命令均有中文/对应语言提示

## 命令（/xrail，别名 /xr）

| 命令 | 说明 | 权限 |
|------|------|------|
| `/xrail help` | 查看帮助 | xrail.use |
| `/xrail status` | 查看各类矿车速度状态 | xrail.use |
| `/xrail set <类型> <速度>` | 设置某类矿车速度上限 | xrail.admin |
| `/xrail vanilla <类型>` | 某类矿车恢复原版 | xrail.admin |
| `/xrail display [方式] [玩家]` | 设置速度显示方式（自己 / 他人） | xrail.display / xrail.admin |
| `/xrail language <语言>` | 切换插件语言（如 zh_cn） | xrail.admin |
| `/xrail reload` | 重载配置并重新应用 | xrail.admin |

矿车类型可用：`minecraft` `chest_minecart` `furnace_minecart` `hopper_minecart` `tnt_minecart` `spawner_minecart`（也支持中文别名：矿车 / 箱子 / 熔炉 / 漏斗 / tnt / 刷怪笼）。

控制台可执行除 `display`（无参数查询）以外的全部命令。

## 配置文件（plugins/XRail/config.yml）

```yaml
language: zh_cn          # 插件语言
limit:
  max: 75                # 全局最高上限（格/秒）
  default: 25            # 默认上限
display:
  enabled: true          # 能否使用速度显示栏
  default-method: ACTIONBAR
  update-interval: 2     # 刷新间隔（tick）
  chat-interval: 20      # CHAT 模式间隔（tick）
carts:
  HOPPER_MINECART:
    enabled: true        # false = 该类型保持原版
    limit: 25            # 速度上限（格/秒）
  ...
```

首次运行还会生成：
- `plugins/XRail/lang/` —— 语言文件夹（zh_cn.yml / en_us.yml，可自行扩展）
- `plugins/XRail/playerdata.yml` —— 玩家显示方式记忆

## 运行要求

- Paper / Purpur 1.20+
- Java 17+（产物以 Java 17 字节码编译）

## 构建

版本号规则：`年.月.日.当日序号`（如 `26.10.27.1` = 2026 年 10 月 27 日第 1 版），
每次构建自动 +1，并同步写入 `build.sh` 的 `VERSION=` 与 `plugin.yml` 的 `version:`。

```bash
# Windows
build.bat
# Linux / macOS
./build.sh
```

产物输出至 `dist/XRail-{版本号}.jar`。

依赖：JDK 21+（`--release 17` 编译），paper-api 1.20.4 等依赖默认读取本地 Maven 仓库（`~/.m2`）。

## Git 规范

- 提交信息：`v{版本号}: 修改说明`
- 标签：`v{版本号}`

## License

MIT
