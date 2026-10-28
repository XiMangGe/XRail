# XRail — 矿车速度控制插件

> 一个功能完整的 Minecraft 矿车速度管理插件，支持按类型限速、潜行+左键+右键 GUI 单车调速、实时速度显示（动作条 / Boss 血条 / 聊天栏），适配 Paper / Purpur 1.20 及以上（含 26.x）。

---

## 📋 目录

- [支持版本](#支持版本)
- [功能特性](#功能特性)
- [安装方法](#安装方法)
- [快速开始](#快速开始)
- [配置文件详解](#配置文件详解)
- [命令大全](#命令大全)
- [权限节点](#权限节点)
- [GUI 调速界面](#gui-调速界面)
- [速度显示栏](#速度显示栏)
- [多语言支持](#多语言支持)
- [常见问题](#常见问题)

---

## 支持版本

| 项目 | 要求 |
|------|------|
| **服务端** | Paper / Purpur **1.20** 及以上（含 1.20.x、1.21.x、26.x） |
| **Java** | Java 17 及以上 |

> 插件按矿车实体类型名匹配，兼容 1.20 的 `COMMAND_MINECART` 与 1.21+ 的 `COMMAND_BLOCK_MINECART`；已针对 26.3 官方 API 编译验证。

---

## 功能特性

### 🚄 按类型限速
- 普通 / 箱子 / 动力 / 漏斗 / TNT / 刷怪笼矿车分别设置速度上限
- 普通矿车最高 **75 格/秒**，其他类型最高 **25 格/秒**
- 某类矿车可一键恢复原版；命令方块矿车始终不受影响
- 区块加载、重启后自动恢复限速，另有周期扫描兜底

### 🖱 GUI 单车调速
- 潜行 + 左键 + 右键（按顺序）点击矿车打开箱子界面
- 第 1 页钻石每个 ±5 格/秒，第 2 页红石粉每个 ±1 格/秒
- 右键加、左键减，箭头翻页，屏障关闭
- 修改只作用于这一台矿车，随矿车存档保存

### 📊 实时速度显示
- 坐进矿车实时显示当前速度（3 位小数，格/秒）
- 三种方式：动作条 / Boss 血条（进度随上限变化）/ 聊天栏定时播报
- 每个玩家自选方式，设置自动保存

### 🌍 多语言
- 内置简体中文 / English，支持自行添加语言文件
- `/xrail language <代码>` 即时切换

### ⚙️ 命令管理
- 管理员设置各类型上限、恢复原版、切换语言、重载
- 所有操作均有游戏内提示，支持 Tab 补全

---

## 安装方法

1. 下载最新版 `XRail-*.jar`：[Releases](https://github.com/XiMangGe/XRail/releases)
2. 将 JAR 文件放入服务器的 `plugins/` 目录
3. 启动服务器，插件自动生成 `config.yml` 与 `lang/` 语言文件夹
4. （可选）修改 `plugins/XRail/config.yml` 进行个性化配置
5. 执行 `/xrail reload` 或重启服务器

---

## 快速开始

安装后默认配置即可使用：

- 所有矿车默认限速 25 格/秒（普通矿车最高可调到 75）
- 坐进矿车，动作条实时显示速度
- 玩家对矿车 `潜行 + 左键 + 右键` 打开调速界面
- 管理员执行 `/xrail status` 查看各类矿车限速状态

---

## 配置文件详解

配置文件路径：`plugins/XRail/config.yml`

### 速度上限

```yaml
limit:
  max-cart: 75     # 普通矿车（MINECART）的最高上限
  max-other: 25    # 其他类型矿车的最高上限
  default: 25      # 未单独配置过的矿车类型的默认上限
```

### 速度显示栏

```yaml
display:
  enabled: true              # 是否允许使用速度显示栏
  default-method: ACTIONBAR  # 默认显示方式: ACTIONBAR / BOSSBAR / CHAT / OFF
  update-interval: 2         # 显示刷新间隔（tick，20 tick = 1 秒）
  chat-interval: 20          # CHAT 模式的发送间隔（tick）
```

### 各类矿车

```yaml
carts:
  MINECART:
    enabled: true   # false = 该类型保持原版
    limit: 25       # 速度上限（格/秒）
  HOPPER_MINECART:
    enabled: true
    limit: 25
  # 其余类型同理：CHEST_MINECART / FURNACE_MINECART / TNT_MINECART / SPAWNER_MINECART
```

> 命令方块矿车（COMMAND_MINECART）始终为原版，不受本插件影响。
>
> 玩家通过 GUI 对单台矿车设置的自定义上限不超过对应类型上限，随矿车存档保存。

---

## 命令大全

主命令：`/xrail`（别名 `/xr`）

| 命令 | 说明 | 权限 |
|------|------|------|
| `/xrail help` | 查看帮助 | xrail.use |
| `/xrail status` | 查看各类矿车速度状态 | xrail.use |
| `/xrail set <类型> <速度>` | 设置某类矿车速度上限 | xrail.admin |
| `/xrail vanilla <类型>` | 某类矿车恢复原版 | xrail.admin |
| `/xrail display [方式] [玩家]` | 设置速度显示（actionbar/bossbar/chat/off） | xrail.display |
| `/xrail language <语言>` | 切换插件语言（如 zh_cn） | xrail.admin |
| `/xrail reload` | 重载配置并重新应用 | xrail.admin |

> **矿车类型**：minecraft / chest / furnace / hopper / tnt / spawner（也支持中文：矿车 / 箱子 / 熔炉 / 漏斗 / tnt / 刷怪笼）。
>
> 控制台可执行除 display 查询外的全部命令；`/xrail display <方式> <玩家>` 修改他人显示需要管理员权限。

---

## 权限节点

| 权限 | 说明 | 默认 |
|------|------|------|
| `xrail.use` | 基础命令与 GUI 调速 | 所有玩家 |
| `xrail.admin` | 管理命令（set/vanilla/language/reload/操作他人显示） | OP |
| `xrail.display` | 切换自己的速度显示方式 | 所有玩家 |

---

## GUI 调速界面

对矿车按顺序 `潜行 → 左键 → 右键`（4 秒内同一台矿车）打开 54 格箱子界面：

| 页面 | 物品 | 操作 |
|------|------|------|
| 第 1 页 | 钻石（数量 = 当前上限 ÷ 5） | 右键 +5 / 左键 -5 格/秒 |
| 第 2 页 | 红石粉（数量 = 当前上限除以 5 的余数） | 右键 +1 / 左键 -1 格/秒 |

- 箭头翻页，屏障关闭，界面内物品不可取走
- 不能超过类型上限（普通矿车 75，其他 25），已达上限会有提示
- 类型被设为原版时无法打开，命令方块矿车不支持调速

---

## 速度显示栏

坐进矿车才显示，下车自动消失：

| 方式 | 效果 |
|------|------|
| `actionbar` | 物品栏上方动作条实时刷新（默认） |
| `bossbar` | 屏幕上方 Boss 血条，进度 = 速度 ÷ 上限 |
| `chat` | 聊天栏定期播报（间隔可配） |
| `off` | 关闭 |

每人用 `/xrail display <方式>` 自选，设置存于 `playerdata.yml`，下次进服依然生效。

---

## 多语言支持

修改 `config.yml` 中的 `language` 字段：

| 值 | 语言 |
|----|------|
| `zh_cn` | 简体中文（默认） |
| `en_us` | English |

首次运行自动生成 `plugins/XRail/lang/` 文件夹；把自定义语言文件（如 `custom.yml`）放进该文件夹，执行 `/xrail language custom` 即可使用。

---

## 常见问题

### Q: 怎么才能冲到 75 格/秒？

原版动力铁轨每次只提供少量加速度，需要**长直道 + 连续充能铁轨**才能把速度推到上限；普通"4 动力 1 普通"的间隔铺法只能维持 8 格/秒左右。弯道和坡道在高速下会脱轨（原版物理，任何限速插件都一样），高速线路请保持直线，进弯前减速。

### Q: 修改限速后重启服务器会失效吗？

不会。矿车的速度上限虽不随存档保存，但插件会在矿车生成、区块实体加载时自动补上，并有周期扫描兜底；通过 GUI 设置的单车上限存在矿车持久化数据（PDC）里，重启不会丢。

### Q: 命令方块矿车能加速吗？

不能，命令方块矿车始终保持原版行为。

### Q: 打开 GUI 提示"该矿车类型已被服务器设为原版速度"？

该类型在 `config.yml` 里 `enabled: false`，让管理员改回 `true`，或执行 `/xrail set <类型> <速度>`。

### Q: 和其他矿车插件冲突吗？

插件只调用 `setMaxSpeed` 接口修改上限，不拦截移动事件，与 TrainCarts 等插件理论上可共存；如遇异常可先 `/xrail vanilla <类型>` 排除变量。

---

## 更新日志

完整更新记录见 [GitHub Releases](https://github.com/XiMangGe/XRail/releases)。
