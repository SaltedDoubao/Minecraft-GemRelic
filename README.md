# GemRelic - 宝石镶嵌与圣遗物属性系统

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x-green)
![Paper](https://img.shields.io/badge/Paper-1.20.x-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![License](https://img.shields.io/badge/License-Custom-red)

一个为 Minecraft Paper 1.20.x 服务器开发的宝石镶嵌与属性系统插件，灵感来源于原神的圣遗物系统。

## 功能特性

### 当前已实现（v1.0.0）

✅ **宝石系统**
- 多种宝石类型
- 每个宝石拥有独特的属性池
- 随机生成属性值
- 宝石等级系统
- 使用 PersistentDataContainer (PDC) 存储宝石数据

✅ **命令系统**
- `/gemrelic give <玩家> <宝石类型> [等级]` - 给予玩家宝石
- `/gemrelic info` - 查看手持宝石的详细信息
- `/gemrelic list` - 列出所有可用的宝石类型
- `/gemrelic reload` - 重载插件配置
- 完整的 Tab 补全支持

✅ **配置系统**
- `gems.yml` - 宝石定义配置
- `config.yml` - 插件主配置
- 支持热重载

✅ **代码规范**
- 完整的 JavaDoc 注释
- 清晰的项目结构
- 遵循 Paper API 最佳实践
- 模块化设计，易于扩展

### 规划中的功能

🔲 **宝石升级系统**
- 消耗材料升级宝石
- 升级成功率机制
- 属性成长系统

🔲 **镶嵌系统**
- 装备宝石槽位
- 宝石安装/卸下
- 宝石属性应用到装备

🔲 **属性加成系统**
- 攻击力加成
- 防御力加成
- 暴击率/暴击伤害
- 生命值/生命恢复
- 其他自定义属性

🔲 **GUI系统**
- 宝石镶嵌界面
- 宝石升级界面
- 宝石背包界面

🔲 **扩展功能**
- 套装效果
- PlaceholderAPI 支持
- 多语言支持
- 数据库存储

## 项目结构

```
Minecraft-GemRelic/
├── src/
│   └── main/
│       ├── java/com/lymc/gemrelic/
│       │   ├── GemRelicPlugin.java          # 插件主类
│       │   ├── command/
│       │   │   └── GemCommand.java          # 命令处理器
│       │   ├── listener/
│       │   │   └── PlayerListener.java      # 事件监听器
│       │   ├── manager/
│       │   │   └── GemManager.java          # 宝石管理器
│       │   └── model/
│       │       ├── GemData.java             # 宝石定义数据
│       │       ├── GemInstance.java         # 宝石实例
│       │       ├── AttributeData.java       # 属性数据
│       │       └── UpgradeConfig.java       # 升级配置
│       └── resources/
│           ├── plugin.yml                   # 插件描述文件
│           ├── config.yml                   # 主配置文件
│           └── gems.yml                     # 宝石配置文件
├── pom.xml                                  # Maven 配置
└── README.md                                # 项目说明
```

## 快速开始

### 环境要求

- Java 17 或更高版本
- Maven 3.6+
- Paper 1.20.x 服务器

### 编译插件

```bash
# 克隆项目
git clone https://github.com/SaltedDoubao/Minecraft-GemRelic.git
cd Minecraft-GemRelic

# 使用 Maven 编译
mvn clean package

# 编译后的 jar 文件位于 target/ 目录
```

### 安装插件

1. 将编译好的 `gemrelic-1.0.0.jar` 放入服务器的 `plugins` 文件夹
2. 重启服务器或使用 PluginManager 加载插件
3. 插件将自动生成配置文件到 `plugins/GemRelic/` 目录

### 配置说明

#### gems.yml

定义宝石类型和属性：

```yaml
gems:
  ruby:                              # 宝石ID
    material: REDSTONE               # 物品材质
    display: "§c红宝石"              # 显示名称
    lore:                            # 物品描述
      - "§7一颗闪耀着红色光芒的宝石"
    attributes:                      # 属性池
      - type: "attack"               # 属性类型
        name: "攻击力"               # 属性名称
        min: 2.0                     # 最小值
        max: 6.0                     # 最大值
```

#### config.yml

主配置文件，包含插件的全局设置。

## 使用示例

### 基本命令

```bash
# 给玩家 一颗 1 级红宝石
/gemrelic give @s ruby 1

# 给玩家 一颗 5 级钻石宝石
/gemrelic give @s diamond_gem 5

# 查看手持宝石的信息
/gemrelic info

# 列出所有可用的宝石类型
/gemrelic list

# 重载配置
/gemrelic reload
```

## 开发文档

### 数据模型

#### GemData
宝石定义数据，从配置文件加载，包含：
- 宝石ID
- 物品材质
- 显示名称和描述
- 属性池定义
- 升级配置

#### GemInstance
宝石实例，表示具体的宝石物品，包含：
- 宝石类型
- 宝石等级
- 实际属性值（从属性池随机生成）

#### AttributeData
属性定义，包含：
- 属性类型
- 属性名称
- 属性值范围（最小值、最大值）

### 管理器

#### GemManager
核心管理器，负责：
- 加载和管理宝石定义
- 创建宝石物品
- 读取物品的宝石数据
- 宝石数据序列化/反序列化

### 扩展指南

要添加新功能，推荐的步骤：

1. 在 `model` 包中定义数据模型
2. 在 `manager` 包中创建对应的管理器
3. 在 `listener` 包中添加事件监听器
4. 在 `command` 包中添加命令支持
5. 在主类中注册新的组件

## 权限节点

| 权限节点 | 说明 | 默认 |
|---------|------|------|
| `gemrelic.command.gem` | 使用 /gemrelic 命令 | OP |
| `gemrelic.command.gem.give` | 给予宝石 | OP |
| `gemrelic.command.gem.upgrade` | 升级宝石 | true |
| `gemrelic.admin` | 管理员权限（包含所有权限） | OP |

## 贡献指南

欢迎提交 [Issue](../../issues) 和 [Pull Request](../../pulls)！

---
