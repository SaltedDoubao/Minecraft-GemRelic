# Minecraft Relic System - 圣遗物属性系统

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x-green)
![Paper](https://img.shields.io/badge/Paper-1.20.x-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![License](https://img.shields.io/badge/License-GPL--3.0-blue)

一个为 Minecraft Paper 1.20.x 服务器开发的圣遗物属性系统插件，用于还原《原神》的圣遗物核心玩法。

## 📚 文档导航
- 用户指南（服主）：[`docs/用户指南.md`](docs/用户指南.md)
- 开发者文档：[`docs/开发者文档.md`](docs/开发者文档.md)
- 项目结构：[`docs/项目结构.md`](docs/项目结构.md)

## 🔎 功能速览（v1.0.0）
- 5部位+套装效果、GUI 装备/仓库
- 随机生成与属性池（主/副词条、权重、步进）
- 宝箱道具（自定义外观、权重抽取）
- 独立存储与数据迁移
- 命令与多层 Tab 补全、AP 兼容

## 🚀 快速开始

### 环境要求
- Java 17 或更高版本
- Maven 3.6+
- Paper 1.20.x 服务器

### 编译插件
```bash
# 克隆项目
git clone https://github.com/SaltedDoubao/Minecraft-Relic-System.git
cd Minecraft-Relic-System

# 编译项目
# 项目提供了编译脚本
.\build_jar.bat 

# 编译后的 jar 文件位于 target/
```

### 安装插件
1. 将编译好的 `relic-system-x.x.x.jar` 放入服务器的 `plugins` 目录
2. 重启服务器或使用 PluginManager 加载插件
3. 插件将自动生成配置到 `plugins/relic-system/` 目录

### 配置与使用
- 配置说明与调优：见 [`docs/用户指南.md`](docs/用户指南.md)
- 全量命令与补全：见 [`docs/用户指南.md`](docs/用户指南.md)
- 进阶与二开：见 [`docs/开发者文档.md`](docs/开发者文档.md)
 
## 🚧 施工计划
 
## 🏛️ 许可证
本项目采用 [GNU GPL v3.0](LICENSE)。
 
## 📜 免责声明
本项目仅供学习交流使用，请勿用于商业用途。
 
## 🤝 贡献
欢迎提交 [Issue](../../issues) 与 [PR](../../pulls)！
