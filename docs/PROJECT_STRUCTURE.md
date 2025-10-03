# GemRelic 项目结构说明

## 📁 项目目录结构

```
Minecraft-GemRelic/
│
├── 📄 pom.xml                                    # Maven构建配置文件
├── 📄 README.md                                  # 项目说明文档
├── 📄 PROJECT_STRUCTURE.md                       # 本文件
├── 📄 .gitignore                                 # Git忽略文件配置
├── 📄 plans.md                                   # 项目设计文档
├── 📄 最小可运行插件骨架设计思路.md                # 骨架设计思路
│
└── 📂 src/main/
    ├── 📂 java/com/lymc/gemrelic/               # Java源代码目录
    │   │
    │   ├── 📄 GemRelicPlugin.java               # 🔴 插件主类
    │   │   └── 职责：插件生命周期管理、组件初始化
    │   │
    │   ├── 📂 model/                             # 数据模型层
    │   │   ├── 📄 GemData.java                  # 宝石定义数据模型
    │   │   ├── 📄 GemInstance.java              # 宝石实例模型
    │   │   ├── 📄 AttributeData.java            # 属性数据模型
    │   │   └── 📄 UpgradeConfig.java            # 升级配置模型
    │   │
    │   ├── 📂 manager/                           # 管理器层
    │   │   └── 📄 GemManager.java               # 🔴 宝石管理器（核心）
    │   │       └── 职责：配置加载、宝石创建、PDC数据读写
    │   │
    │   ├── 📂 command/                           # 命令处理层
    │   │   └── 📄 GemCommand.java               # /gem命令处理器
    │   │       └── 职责：命令解析、Tab补全、权限检查
    │   │
    │   └── 📂 listener/                          # 事件监听层
    │       └── 📄 PlayerListener.java           # 玩家事件监听器
    │           └── 职责：玩家加入/退出事件（预留扩展）
    │
    └── 📂 resources/                             # 资源文件目录
        ├── 📄 plugin.yml                        # 插件描述文件（必需）
        ├── 📄 config.yml                        # 插件主配置文件
        └── 📄 gems.yml                          # 宝石定义配置文件
```

## 🔧 核心组件说明

### 1️⃣ 插件主类 (GemRelicPlugin.java)

**继承关系**: `extends JavaPlugin`

**核心方法**:
- `onEnable()` - 插件启动时执行
- `onDisable()` - 插件关闭时执行
- `reloadPluginConfig()` - 重载配置

**初始化流程**:
```
onEnable()
  ├── 创建数据文件夹
  ├── 保存默认配置
  ├── 初始化 GemManager
  ├── 注册命令处理器
  └── 注册事件监听器
```

### 2️⃣ 数据模型层 (model/)

#### GemData（宝石定义）
- 从配置文件加载的宝石模板
- 包含：ID、材质、显示名称、属性池、升级配置
- 不可变对象，线程安全

#### GemInstance（宝石实例）
- 代表具体的宝石物品
- 包含：宝石类型、等级、实际属性值
- 可变对象，支持属性修改和升级

#### AttributeData（属性定义）
- 定义属性的类型、名称和数值范围
- 提供随机属性值生成方法

#### UpgradeConfig（升级配置）
- 定义升级所需材料、成功率、成长倍率
- 用于后续升级系统扩展

### 3️⃣ 宝石管理器 (GemManager.java)

**核心功能**:
- ✅ 从 `gems.yml` 加载宝石定义
- ✅ 创建带有随机属性的宝石物品
- ✅ 使用 PersistentDataContainer 存储宝石数据
- ✅ 从 ItemStack 读取宝石信息
- ✅ 支持热重载配置

**PDC 数据结构**:
```
NamespacedKey: gemrelic:gem_type      -> String  (宝石类型ID)
NamespacedKey: gemrelic:gem_level     -> Integer (宝石等级)
NamespacedKey: gemrelic:gem_attributes -> String  (属性序列化字符串)
```

**属性序列化格式**:
```
"attack:5.2;defense:8.1;crit_rate:3.5"
```

### 4️⃣ 命令处理器 (GemCommand.java)

**实现接口**: `CommandExecutor`, `TabCompleter`

**子命令**:
| 命令 | 权限 | 说明 |
|------|------|------|
| `/gem give <玩家> <类型> [等级]` | `gemrelic.command.gem.give` | 给予宝石 |
| `/gem info` | - | 查看手持宝石信息 |
| `/gem list` | - | 列出所有宝石类型 |
| `/gem reload` | `gemrelic.admin` | 重载配置 |
| `/gem help` | - | 显示帮助 |

**Tab补全支持**:
- 第1参数：子命令补全
- 第2参数（give）：在线玩家补全
- 第3参数（give）：宝石类型补全
- 第4参数（give）：等级建议

### 5️⃣ 事件监听器 (PlayerListener.java)

**当前监听事件**:
- `PlayerJoinEvent` - 玩家加入（目前仅日志）
- `PlayerQuitEvent` - 玩家退出（目前仅日志）

**预留扩展**:
- `EntityDamageByEntityEvent` - 攻击伤害加成
- `EntityDamageEvent` - 防御加成
- `PlayerInteractEvent` - 宝石镶嵌交互
- `InventoryClickEvent` - GUI点击事件

## 📋 配置文件说明

### plugin.yml
```yaml
name: GemRelic              # 插件名称
version: 1.0.0              # 插件版本
main: com.lymc.gemrelic.GemRelicPlugin  # 主类路径
api-version: '1.20'         # API版本
commands:                   # 命令定义
  gem: ...                  
permissions:                # 权限定义
  gemrelic.command.gem: ...
```

### gems.yml
```yaml
gems:
  ruby:                     # 宝石ID
    material: REDSTONE      # 物品材质
    display: "§c红宝石"     # 显示名称
    lore: [...]            # 物品描述
    attributes:            # 属性池
      - type: "attack"
        name: "攻击力"
        min: 2.0
        max: 6.0
    upgrade:               # 升级配置
      cost_material: DIAMOND
      cost_amount: 1
      success_chance: 0.8
      growth_rate: 1.2
```

### config.yml
```yaml
settings:                  # 插件设置
  debug: false
  language: zh_CN
gem:                       # 宝石系统设置
  max_level: 100
  lose_gem_on_fail: false
socket:                    # 镶嵌系统设置（预留）
  max_slots: 3
  allow_remove: true
attribute:                 # 属性系统设置（预留）
  enabled: true
  calculation_mode: additive
messages:                  # 消息配置
  prefix: "§6[GemRelic]§r "
```

## 🔑 关键设计模式

### 1. 单例模式
```java
// GemRelicPlugin 使用单例模式
private static GemRelicPlugin instance;
public static GemRelicPlugin getInstance() {
    return instance;
}
```

### 2. 管理器模式
- 每个功能模块都有对应的 Manager
- Manager 负责该模块的数据加载、业务逻辑
- 通过插件主类获取 Manager 实例

### 3. 数据传输对象 (DTO)
- GemData、GemInstance 等都是纯数据对象
- 封装数据，提供 getter/setter
- 不包含业务逻辑

### 4. 命令-处理器模式
- 每个命令都有独立的处理方法
- 通过 switch-case 分发到对应的处理器
- 便于扩展新的子命令

## 📊 数据流图

### 宝石创建流程
```
玩家执行命令 → GemCommand.handleGive()
                    ↓
            GemManager.createGem()
                    ↓
            读取 GemData（配置）
                    ↓
            随机生成属性值
                    ↓
            创建 GemInstance
                    ↓
            序列化到 PDC
                    ↓
            生成 ItemStack
                    ↓
            给予玩家
```

### 宝石信息读取流程
```
玩家手持物品 → GemCommand.handleInfo()
                    ↓
            GemManager.isGem()
                    ↓
            读取 PDC 数据
                    ↓
            反序列化属性
                    ↓
            创建 GemInstance
                    ↓
            显示信息给玩家
```

## 🚀 编译与部署

### 编译命令
```bash
mvn clean package
```

### 输出位置
```
target/gemrelic-1.0.0.jar
```

### 部署步骤
1. 将 jar 文件放入服务器 `plugins/` 目录
2. 重启服务器或使用插件加载器
3. 配置文件自动生成到 `plugins/GemRelic/`

## 📈 后续扩展方向

### 短期目标
1. ✅ 宝石基础系统
2. 🔲 宝石升级功能
3. 🔲 宝石镶嵌GUI
4. 🔲 属性加成系统

### 中期目标
1. 🔲 装备槽位系统
2. 🔲 镶嵌/拆卸功能
3. 🔲 套装效果
4. 🔲 数据持久化（SQLite/MySQL）

### 长期目标
1. 🔲 PlaceholderAPI 集成
2. 🔲 多语言支持
3. 🔲 经济系统集成
4. 🔲 自定义模型（ResourcePack）

## 🎯 代码规范检查清单

- ✅ 所有公共方法都有 JavaDoc 注释
- ✅ 使用 PersistentDataContainer 存储数据
- ✅ 配置文件支持热重载
- ✅ 完整的错误处理和日志记录
- ✅ 命令权限检查
- ✅ Tab 补全实现
- ✅ 遵循 Paper API 最佳实践
- ✅ 模块化设计，低耦合高内聚

## 📝 维护建议

1. **定期更新依赖**: 检查 Paper API 版本更新
2. **性能优化**: 监控 PDC 读写性能
3. **配置验证**: 添加配置文件合法性检查
4. **单元测试**: 为核心逻辑添加测试用例
5. **文档更新**: 保持文档与代码同步

---

**创建时间**: 2025-10-03  
**插件版本**: 1.0.0  
**最后更新**: 2025-10-03

