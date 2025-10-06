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
- **多种交互**: 拖动放入、Shift-点击快速存入
- 分页浏览、部位筛选、操作提示、空仓库智能提示

✅ **数据存储系统** 🆕
- **独立存储系统**（推荐）: 类似末影箱原理但不占用末影箱空间
  - 采用二进制DAT文件，利用Minecraft原生序列化
  - 数据实时持久化，可靠性100%
  - 存储路径: `plugins/GemRelic/relic_storage/<uuid>.dat`
- **YAML存储**（向后兼容）: 传统文件存储方式
- **自动数据迁移**: 登录时自动从YAML迁移到独立存储
- 圣遗物↔ItemStack双向转换
- PersistentDataContainer嵌入式存储

✅ **命令系统**
- `/relic open` - 打开圣遗物系统主菜单
- `/relic list` - 查看已配置的套装
- `/relic test` - 添加测试圣遗物（仓库+背包）
- `/relic reload` - 重载配置
- `/relic migrate` - 数据迁移工具（管理员）
- `/relic migration-status` - 查看迁移状态（管理员）
- **三层 Tab 补全**: 子命令 → 部位 → 套装ID

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

请见文档：《docs/项目结构.md》。

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
  storage_mode: INVENTORY            # 存储方式: YAML | INVENTORY
  
  # 独立存储设置（推荐）
  inventory_storage:
    auto_migrate: true               # 自动迁移YAML数据
    keep_backup: true                # 保留备份
    file_format: DAT                 # 文件格式

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
- **拖动放入** 🆕 → 将光标上的圣遗物拖入仓库区域放入
- **Shift-点击** 🆕 → 在背包中Shift-点击圣遗物快速存入
- **筛选按钮** → 按部位筛选显示
- **翻页按钮** → 浏览更多圣遗物
- **放入按钮** → 从背包收集所有圣遗物物品

### 命令列表

#### 玩家命令
```bash
# 打开圣遗物系统主菜单
/relic open

# 查看已配置的套装
/relic list

# 添加测试圣遗物（仓库5件+背包3件）
/relic test

# 装备圣遗物（测试用）
/relic equip <部位> <套装ID>

# 卸下圣遗物（测试用）
/relic unequip <部位>
```

#### 管理员命令
```bash
# 重载配置文件
/relic reload

# 数据迁移到独立存储系统
/relic migrate

# 查看数据迁移状态
/relic migration-status
```

## 开发文档

开发者请阅读：《docs/开发者文档.md》。其中“项目结构”部分指向《docs/项目结构.md》。

## 权限节点

| 权限节点 | 说明 | 默认 |
|---------|------|------|
| `gemrelic.user` | 使用 /relic 基础命令 | true |
| `gemrelic.admin` | 管理员权限（重载配置等） | OP |

## 贡献指南

欢迎提交 [Issue](../../issues) 和 [Pull Request](../../pulls)！

---
