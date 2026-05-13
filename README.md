# OpenYGT-DMS 智能煎药管理系统

[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.0-green.svg)]()
[![Docker](https://img.shields.io/badge/docker--compose-ready-2496ED.svg)]()
[![Vue](https://img.shields.io/badge/Vue-3-4FC08D.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-6DB33F.svg)]()

面向中药房汤剂煎煮场景的全流程智能管理系统，覆盖从处方接收到患者取药的完整链路。

> 系统内置设备物联网对接能力，支持煎药机、包装机、标签打印机等设备的实时监控与智能调度。

---

## 🚀 Demo 在线体验

| 项目 | 地址 |
|------|------|
| 🌐 演示站点 | https://demo.dms.openygt.org.cn |
| 👤 用户名 | `admin` |
| 🔑 密码 | `admin123` |

> 演示数据每日凌晨自动重置，如遇无法登录请稍后再试。

### 主要功能预览

| 模块 | 功能说明 |
|------|----------|
| 📋 处方管理 | 处方录入、接收、审核、流转全流程 |
| 🏭 生产指挥 | 生产看板、任务分配、流程跟踪、时效监控 |
| 🔧 设备管理 | 设备台账、联网监控、分组配对、告警中心 |
| ✅ 质量检验 | 质检记录、留样管理、合格统计 |
| 📦 发药管理 | 成品暂存、发药确认、患者进度查询 |
| 🔍 追溯查询 | 处方追溯、批次追溯、异常追溯 |
| 📱 PDA 作业 | 移动端煎药操作与设备识别 |
| 🖨️ 打印中心 | 标签打印、工单打印管理 |
| ⚙️ 系统管理 | 用户、角色、菜单、权限、参数配置 |

---

## 📦 快速开始

### 方式一：Docker 一键部署（推荐）

> 支持 Windows、macOS、Linux

**前置要求**
- [Docker](https://www.docker.com/products/docker-desktop) 20.10+
- [Docker Compose](https://docs.docker.com/compose/install/) 2.0+

**安装步骤**

```bash
# 1. 克隆仓库
git clone https://gitee.com/openygt/openygt-dms.git
cd openygt-dms

# 2. 启动全部服务（MySQL + Redis + 后端 + 前端）
docker compose -f docker-compose.oss.yml up -d

# 3. 等待服务就绪（首次启动约 60 秒）
docker compose -f docker-compose.oss.yml logs -f backend

# 4. 访问系统
# Web 管理端：http://localhost
# 默认账号：admin / admin123
```

**常用命令**

```bash
# 查看服务状态
docker compose -f docker-compose.oss.yml ps

# 查看日志
docker compose -f docker-compose.oss.yml logs -f backend

# 停止服务
docker compose -f docker-compose.oss.yml down

# 完全清理（包括数据卷）
docker compose -f docker-compose.oss.yml down -v
```

---

### 方式二：源码安装

#### 前置环境

| 组件 | 版本要求 | 下载地址 |
|------|---------|----------|
| JDK | 8+ | [Oracle](https://www.oracle.com/java/technologies/downloads/) / [OpenJDK](https://adoptium.net/) |
| Node.js | 18+ | [nodejs.org](https://nodejs.org/) |
| MySQL | 8.0+ | [mysql.com](https://dev.mysql.com/downloads/) |
| Redis | 7+ | [redis.io](https://redis.io/download/) |
| Maven | 3.8+ | [maven.apache.org](https://maven.apache.org/download.cgi) |

#### Windows

```powershell
# 1. 克隆仓库
git clone https://gitee.com/openygt/openygt-dms.git
cd openygt-dms

# 2. 创建数据库（使用 MySQL 客户端或工具）
# CREATE DATABASE openygt_dms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 3. 启动后端
# 设置环境变量：JWT_SECRET=your-secret-key-at-least-32-characters
$env:JWT_SECRET="your-secret-key-at-least-32-characters"
cd dms-app
target\dms-app-1.0.0.jar --server.port=8080
# 或使用 Maven: mvn spring-boot:run -pl dms-app

# 4. 启动前端（新开 PowerShell 窗口）
cd frontend
npm install
npm run dev

# 5. 访问 http://localhost:5173
```

#### macOS / Linux

```bash
# 1. 克隆仓库
git clone https://gitee.com/openygt/openygt-dms.git
cd openygt-dms

# 2. 创建数据库
mysql -uroot -p -e "CREATE DATABASE openygt_dms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. 编译后端
mvn clean package -DskipTests

# 4. 启动后端
export JWT_SECRET="your-secret-key-at-least-32-characters"
java -jar dms-app/target/dms-app-1.0.0.jar --server.port=8080

# 5. 启动前端（新开终端窗口）
cd frontend
npm install
npm run dev

# 6. 访问 http://localhost:5173
```

---

## 🏗️ 项目结构

```
openygt-dms/
├── dms-analytics/          # 数据分析模块
├── dms-app/                # 应用启动入口
│   └── src/main/resources/
│       ├── db/migration/   # Flyway 数据库迁移脚本
│       └── db/demo/        # 演示数据
├── dms-common/             # 公共模块（工具类、常量）
├── dms-equipment/          # 设备管理模块
├── dms-inventory/          # 库存管理模块
├── dms-iot-gateway/        # IoT 网关模块
├── dms-masterdata/         # 主数据模块
├── dms-outpatient/         # 门诊模块
├── dms-pda/                # PDA 移动端
├── dms-print/              # 打印中心模块
├── dms-production/         # 生产管理模块
├── dms-quality/            # 质量检验模块
├── dms-rbac/               # 权限控制模块
├── dms-system/             # 系统管理模块
├── frontend/               # Web 前端（Vue 3 + Vite）
│   └── src/
│       ├── api/            # API 接口
│       ├── views/          # 页面组件
│       └── router/         # 路由配置
├── pda-uniapp/             # PDA 移动端（uni-app）
├── docs/                   # 文档目录
└── tests/                  # 测试目录
```

---

## 🛠️ 技术栈

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| 前端（Web） | Vue 3 + TypeScript + Element Plus | Vue 3.4 |
| 前端（PDA） | uni-app（Vue 3） | — |
| 后端 | Spring Boot + MyBatis-Plus | Spring Boot 2.7.18 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7+ |
| 消息 | MQTT | — |
| 构建 | Maven | 3.8+ |
| 部署 | Docker + Docker Compose | 20.10+ |

---

## 📖 文档

- **用户手册 & 部署指南** → [openygt-docs](https://gitee.com/openygt/openygt-docs)
- **在线阅读** → https://openygt.org.cn/docs

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

- 提交前请阅读 [CONTRIBUTING.md](./CONTRIBUTING.md)
- 提交 PR 需签署 [CLA.md](./CLA.md)
- 代码提交请遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范

---

## 📜 开源协议

OpenYGT-DMS 采用 **双协议模式**：

| 版本 | 协议 | 适用场景 |
|------|------|----------|
| 开源版 | [AGPL-3.0](LICENSE) | 免费用，SaaS/云端部署需开源修改代码 |
| 商业版 | [商业授权协议](LICENSE.commercial.md) | 医院闭源部署、豁免开源义务 |

> 如需商业授权，请联系：luobin@openygt.org.cn

---

## 💬 社区与支持

| 渠道 | 适用场景 |
|------|----------|
| 🐛 [Gitee Issues](https://gitee.com/openygt/openygt-dms/issues) | Bug 报告、功能建议 |
| 📧 luobin@openygt.org.cn | 商务合作、设备对接 |
| 💬 社区微信 openygt | 日常交流、提问 |

---

<p align="center">
  <sub>Copyright © 2026 OpenYGT Contributors & 上海周方智能科技有限公司</sub>
</p>
