# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个为 Minecraft Paper 1.20.x 服务器开发的圣遗物属性系统插件，用于还原《原神》的圣遗物核心玩法。插件支持5部位圣遗物装备、套装效果、随机属性生成、宝箱系统，并提供内置属性引擎与可选的 AttributePlus 集成。

- **语言**: 所有响应必须使用中文简体
- **Java 版本**: Java 17
- **Minecraft 版本**: Paper 1.20.x

## 常用命令

### 构建与编译
```bash
# 使用项目提供的编译脚本（推荐）
.\build_jar.bat

# 编译产物位置
# target/
```

### 测试与调试
本项目当前无自动化测试。调试需要：
1. 将编译好的 JAR 部署到 Paper 1.20.x 测试服务器的 `plugins/` 目录
2. 启动/重载服务器
3. 使用游戏内命令进行功能验证：
   - `/relic help` - 查看所有命令
   - `/relic list` - 查看已配置的套装
   - `/relic gen <setId> <slot> <rarity> <level>` - 生成测试圣遗物
   - `/relic box give <player> <boxId> [amount]` - 发放宝箱

## 架构概览

项目采用分层架构设计：

### 核心层次
1. **数据模型层** (`relic/`)
   - `RelicData`: 圣遗物实体数据（套装、部位、品质、等级、主副词条）
   - `RelicSlot`: 5个部位枚举（FLOWER/PLUME/SANDS/GOBLET/CIRCLET）
   - `RelicRarity`: 品质枚举（1-5星）
   - `RelicStatType`: 属性类型枚举（生命、攻击、暴击等）
   - `PlayerRelicProfile`: 玩家的装备状态与仓库存储

2. **存储层** (`storage/`)
   - 支持两种存储模式：
     - `YAML`: 传统文件存储（`RelicProfileManager`）
     - `INVENTORY`: 独立存储系统，类似末影箱原理（`InventoryProfileManager`）
   - `StorageFactory`: 统一创建存储管理器
   - `DataMigration`: 处理 YAML 到 INVENTORY 的数据迁移

3. **业务服务层** (`service/`)
   - `RelicEffectService`: 套装效果与属性应用（支持原版属性修饰与 AP 集成）
   - `StatAggregationService`: 聚合主副词条统计
   - `RelicGenerationService`: 随机生成圣遗物（基于属性池配置）
   - `AttributePlusBridge`: AttributePlus 插件桥接（可选集成）

4. **管理层** (`manager/`)
   - `RelicManager`: 加载套装与属性池配置
   - `TreasureBoxManager`: 宝箱系统管理

5. **界面层** (`gui/`)
   - `RelicMainMenuGUI`: 主菜单界面
   - `RelicEquipmentGUI`: 装备界面（5个装备槽位）
   - `RelicWarehouseGUI`: 仓库界面（翻页展示）

6. **监听器层** (`listener/`)
   - `PlayerListener`: 玩家登录/退出处理（加载/保存数据，应用属性）
   - `RelicGUIListener`: GUI 交互监听
   - `TreasureBoxListener`: 宝箱右键开启监听
   - `CombatListener`: 内置战斗计算（暴击、伤害加成等，仅在未启用 AP 时注册）

7. **命令层** (`command/`)
   - `RelicCommand`: 主命令处理器，支持多层 Tab 补全
   - 子命令：help, list, open, gen, give, box, reload, migrate, migration-status

### 关键设计决策

#### 属性引擎双模式
- **内置模式**（默认）：不依赖任何外部插件，使用原版 Attribute API 应用攻速、移速、幸运等，通过 `CombatListener` 处理暴击和伤害计算
- **AP 集成模式**（可选）：当 `config.yml -> integration.attributeplus.enabled = true` 且服务器安装了 AttributePlus 时：
  - 内置属性应用与战斗监听器自动禁用
  - 通过 `AttributePlusBridge` 将圣遗物属性转换为 AP 命名空间属性
  - 完全由 AP 处理属性展示与战斗计算，避免重复结算

#### 存储系统演进
- 传统 `YAML` 模式：每个玩家一个 YAML 文件，装备与仓库数据保存在同一文件
- 新的 `INVENTORY` 模式（推荐）：
  - 使用类似末影箱的独立存储原理（但不占用末影箱）
  - 每个玩家有独立的装备与仓库容器
  - 支持 DAT（二进制）或 YAML 格式
  - 自动迁移：首次切换时可自动从 YAML 迁移数据

#### 随机生成系统
- 配置驱动：`relics/attribute_pool.yml` 定义：
  - 主词条：基础值、每级步进、允许部位
  - 副词条：权重、初始/强化数值池
  - 品质配置：最大等级、初始副词条数量范围
- 生成规则：
  - FLOWER 固定生命值，PLUME 固定攻击力，其他部位从候选池随机
  - 主词条随等级线性增长（可配置）
  - 副词条加权随机，自动去重（不与主词条和已有副词条重复）
  - 每5级触发一次副词条新增或强化

## 配置文件结构

- `config.yml`: 主配置（调试开关、存储模式、AP 集成配置）
- `relics/sets.yml`: 套装定义（套装ID、名称、描述）
- `relics/attribute_pool.yml`: 属性池配置（主副词条数值、权重、步进）
- `relics/treasure_box.yml`: 宝箱配置（外观、掉落权重）

## 命名空间约定

- AP 集成命名空间：默认 `RelicSystem`，在 `config.yml -> integration.attributeplus.namespace` 配置
- 原版属性修饰器命名格式：`relic:<type>:<detail>`（如 `relic:gladiator:2pc`, `relic:stat:HP_FLAT`）

## 开发注意事项

1. **互斥原则**：当 AP 集成启用时，内置属性应用与 `CombatListener` 自动禁用，避免重复计算
2. **数值口径**：配置文件中百分比数值按"百分数"理解（如 5.0 表示 5%）
3. **原版属性限制**：移速和攻速修饰可能受服务端/客户端上限限制，需合理控制数值
4. **线程安全**：暴击计算使用 `ThreadLocalRandom`，缓存使用 `HashMap`（单线程访问）
5. **属性清理**：插件禁用时必须清理所有在线玩家的属性修饰（包括原版与 AP），避免残留

## 扩展指南

- **新增属性类型**：在 `RelicStatType` 枚举中添加，更新 `attribute_pool.yml`，在 `RelicEffectService` 或 `AttributePlusBridge.stat_map` 中映射
- **自定义套装效果**：在 `RelicEffectService.refresh()` 中添加套装计数逻辑
- **伤害类型细分**：在 `CombatListener` 中按伤害来源（近战/箭矢/魔法）实现不同乘区

## 依赖说明

- **必需依赖**：Paper API 1.20.1（provided scope）
- **软依赖**：AttributePlus（可选，在 `plugin.yml` 中声明为 `softdepend`）
- **构建工具**：Maven 3.6+
- **打包插件**：maven-shade-plugin（用于生成包含依赖的最终 JAR）

## 相关文档

- [用户指南.md](docs/用户指南.md) - 完整命令列表与配置说明
- [开发者文档.md](docs/开发者文档.md) - 详细的模块对接与扩展指南
- [项目结构.md](docs/项目结构.md) - 文件结构详细说明
