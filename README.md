# GemRelic - 圣遗物属性系统

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x-green)
![Paper](https://img.shields.io/badge/Paper-1.20.x-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![License](https://img.shields.io/badge/License-Custom-red)

一个为 Minecraft Paper 1.20.x 服务器开发的圣遗物属性系统插件，高度还原《原神》的圣遗物核心玩法。

## 功能特性

### 当前已实现（v1.0.0）

✅ **圣遗物5部位系统**
- 生之花 (FLOWER) - 固定生命值主词条
- 死之羽 (PLUME) - 固定攻击力主词条  
- 时之沙 (SANDS) - 可变主词条（攻击%/生命%/防御%等）
- 空之杯 (GOBLET) - 可变主词条（元素伤害/攻击%等）
- 理之冠 (CIRCLET) - 可变主词条（暴击率/暴击伤害等）

✅ **套装效果系统**
- 2件套装效果自动激活
- 4件套装效果自动激活
- 实时属性修饰应用（原版Attribute系统）
- 套装件数统计与显示

✅ **GUI界面系统**
- **主菜单**: 系统总入口，连接各功能页面
- **装备页面**: 查看已装备圣遗物，支持卸下操作
- **仓库页面**: 管理仓库圣遗物，支持装备/取出到背包
- 分页浏览、部位筛选、操作提示

✅ **数据存储系统**
- 玩家档案YAML持久化 (`plugins/GemRelic/players/<uuid>.yml`)
- 圣遗物↔ItemStack双向转换
- PersistentDataContainer嵌入式存储

✅ **命令系统**
- `/relic open` - 打开圣遗物系统主菜单
- `/relic list` - 查看已配置的套装
- `/relic test` - 添加测试圣遗物（调试用）
- `/relic reload` - 重载配置
- 完整的 Tab 补全支持

✅ **AttributePlus兼容**
- 软依赖支持，自动检测AP插件
- 配置化属性映射（暴击率、攻速、移速等）
- 优雅降级（未安装AP时仍正常工作）

### 规划中的功能

🔲 **随机生成系统**
- pools.yml - 主副词条权重配置
- rarity.yml - 1-5星稀有度与等级上限
- drops.yml - 掉落源配置

🔲 **强化系统**  
- 经验曲线与升级
- +5级跳副词条机制
- 圣遗物分解与精粹

🔲 **高级功能**
- 圣遗物锁定防护
- 套装图鉴与收集进度
- 副词条重掷与洗练

## 项目结构

```
Minecraft-GemRelic/
├── src/
│   └── main/
│       ├── java/com/lymc/gemrelic/
│       │   ├── GemRelicPlugin.java          # 插件主类
│       │   ├── command/
│       │   │   └── RelicCommand.java        # 圣遗物命令处理器
│       │   ├── gui/
│       │   │   ├── RelicMainMenuGUI.java    # 主菜单界面
│       │   │   ├── RelicEquipmentGUI.java   # 装备管理界面
│       │   │   └── RelicWarehouseGUI.java   # 仓库管理界面
│       │   ├── listener/
│       │   │   ├── PlayerListener.java      # 玩家事件监听器
│       │   │   └── RelicGUIListener.java    # GUI交互监听器
│       │   ├── manager/
│       │   │   ├── RelicManager.java        # 圣遗物管理器
│       │   │   └── RelicProfileManager.java # 玩家档案管理器
│       │   ├── relic/
│       │   │   ├── RelicData.java           # 圣遗物数据模型
│       │   │   ├── RelicSlot.java           # 部位枚举
│       │   │   ├── RelicRarity.java         # 稀有度枚举
│       │   │   ├── RelicStatType.java       # 属性类型枚举
│       │   │   └── PlayerRelicProfile.java  # 玩家档案
│       │   ├── service/
│       │   │   ├── RelicEffectService.java  # 套装效果服务
│       │   │   └── AttributePlusBridge.java # AP兼容桥接
│       │   └── util/
│       │       ├── RelicIO.java             # 序列化工具
│       │       └── RelicItemConverter.java  # 物品转换器
│       └── resources/
│           ├── plugin.yml                   # 插件描述文件
│           ├── config.yml                   # 主配置文件
│           └── relics/
│               └── sets.yml                 # 套装定义文件
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

#### relics/sets.yml

定义套装类型和效果：

```yaml
sets:
  gladiator:                         # 套装ID
    id: gladiator                    # 内部ID
    name: "角斗士的终幕礼"            # 套装显示名称
    bonuses:                         # 套装效果
      two_piece_desc:                # 2件套装效果描述
        - "攻击力+18%"
      four_piece_desc:               # 4件套装效果描述  
        - "持剑/双手剑/长柄武器时，普通攻击造成的伤害提升35%"
```

#### config.yml

主配置文件，包含圣遗物系统设置和AttributePlus集成配置：

```yaml
# 圣遗物系统设置
relic:
  enabled: true                      # 是否启用系统
  warehouse_max_size: 1000           # 仓库最大容量

# AttributePlus集成
integration:
  attributeplus:
    enabled: true                    # 是否启用AP集成
    stat_map:                        # 属性映射表
      CRIT_RATE: crit_chance         # 暴击率 → AP暴击概率
      ATK_SPEED: attack_speed        # 攻速 → AP攻击速度
```

## 使用示例

### 基本操作流程

```bash
# 1. 添加测试圣遗物到仓库
/relic test

# 2. 打开圣遗物系统主菜单
/relic open

# 3. GUI操作：
# - 点击"装备管理" → 查看当前穿戴
# - 点击"仓库管理" → 管理圣遗物
```

### GUI操作指南

#### 主菜单操作
- 🛡️ **装备管理** - 查看和管理已装备的5件圣遗物
- 📦 **仓库管理** - 管理仓库中的圣遗物

#### 装备页面操作  
- **点击已装备圣遗物** → 卸下到仓库
- **查看套装加成** → 右侧显示当前套装件数与效果

#### 仓库页面操作
- **左键圣遗物** → 装备到对应部位
- **右键圣遗物** → 取出到背包
- **筛选按钮** → 按部位筛选显示
- **翻页按钮** → 浏览更多圣遗物
- **放入按钮** → 从背包收集所有圣遗物物品

### 管理员命令

```bash
# 查看已配置的套装
/relic list

# 重载配置文件
/relic reload

# 添加测试圣遗物（调试用）
/relic test
```

## 开发文档

### 核心架构

#### 数据模型层 (relic/)
- **RelicData**: 单件圣遗物完整数据
- **RelicSlot**: 5个部位枚举
- **RelicRarity**: 1-5星稀有度
- **RelicStatType**: 主副词条类型定义
- **PlayerRelicProfile**: 玩家档案（已装备+仓库）

#### 管理层 (manager/)
- **RelicManager**: 套装配置管理
- **RelicProfileManager**: 玩家档案持久化

#### 服务层 (service/)
- **RelicEffectService**: 套装效果计算与应用
- **AttributePlusBridge**: AP插件兼容桥接
- **StatAggregationService**: 属性聚合计算

#### GUI层 (gui/)
- **RelicMainMenuGUI**: 系统主菜单
- **RelicEquipmentGUI**: 装备管理界面
- **RelicWarehouseGUI**: 仓库管理界面

### 扩展指南

圣遗物系统采用模块化设计，扩展新功能的推荐步骤：

1. **数据模型**: 在 `relic/` 包中定义新的数据结构
2. **业务逻辑**: 在 `service/` 包中实现核心逻辑  
3. **数据管理**: 在 `manager/` 包中添加数据管理
4. **用户界面**: 在 `gui/` 包中创建交互界面
5. **主类注册**: 在 `GemRelicPlugin.java` 中注册新组件

## 权限节点

| 权限节点 | 说明 | 默认 |
|---------|------|------|
| `gemrelic.user` | 使用 /relic 基础命令 | true |
| `gemrelic.admin` | 管理员权限（重载配置等） | OP |

## 贡献指南

欢迎提交 [Issue](../../issues) 和 [Pull Request](../../pulls)！

---
