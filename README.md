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
| 💬 体验账号 | 请微信联系 `openygt` 获取账号与密码 |

> 演示数据每日凌晨自动重置，如需体验请添加微信 `openygt` 申请试用账号。

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

### 方式一：Docker 一键部署（推荐 ⭐）

> 适合：想最快体验系统的用户，无需安装 JDK、Node.js、MySQL
>
> 支持：Windows、macOS、Linux

**前置要求**
- [Docker](https://www.docker.com/products/docker-desktop) 20.10+
- [Docker Compose](https://docs.docker.com/compose/install/) 2.0+

**安装步骤**

```bash
# 1. 克隆仓库
git clone https://gitee.com/openygt/openygt-dms.git
cd openygt-dms

# 2. 设置 JWT 密钥（至少 32 位随机字符串，不能包含 "Test"）
export JWT_SECRET="YourRandomSecretKeyAtLeast32CharactersLong"

# 3. 启动全部服务（MySQL + Redis + 后端 + 前端）
docker compose up -d

# 4. 等待服务就绪（首次启动约 60-90 秒）
docker compose logs -f backend

# 5. 访问系统
# Web 管理端：http://localhost
# 默认账号：admin / admin123
```

> **Windows 用户注意**：`export` 命令请替换为 `set JWT_SECRET=你的密钥`

**常用命令**

```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f backend
docker compose logs -f frontend

# 停止服务
docker compose down

# 完全清理（包括数据库数据）
docker compose down -v
```

---

### 方式二：源码安装

> 适合：开发者、需要二次定制、想深入了解系统的用户

#### 前置环境

| 组件 | 版本要求 | 下载地址 |
|------|---------|----------|
| JDK | 8+ | [Oracle](https://www.oracle.com/java/technologies/downloads/) / [OpenJDK](https://adoptium.net/) |
| Node.js | 18+ | [nodejs.org](https://nodejs.org/) |
| MySQL | 8.0+ | [mysql.com](https://dev.mysql.com/downloads/) |
| Redis | 7+ | [redis.io](https://redis.io/download/) |
| Maven | 3.8+ | [maven.apache.org](https://maven.apache.org/download.cgi) |

#### 第一步：配置后端

```bash
# 1. 克隆仓库
git clone https://gitee.com/openygt/openygt-dms.git
cd openygt-dms

# 2. 复制示例配置文件
cp dms-app/src/main/resources/application.yml.example \
   dms-app/src/main/resources/application.yml

# 3. 编辑 application.yml，修改以下配置：
#    - spring.datasource.password: 你的 MySQL 密码
#    - spring.redis.password: 你的 Redis 密码（如无密码可留空）
#    - jwt.secret: 至少 32 位的随机字符串（不能包含 "Test"）
```

#### 第二步：创建数据库

```bash
# 使用 MySQL 客户端创建数据库
mysql -uroot -p -e "CREATE DATABASE openygt_dms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

#### 第三步：启动后端

```bash
# 编译后端
mvn clean package -DskipTests

# 设置环境变量并启动
export JWT_SECRET="YourRandomSecretKeyAtLeast32CharactersLong"
java -jar dms-app/target/dms-app-1.0.0.jar --server.port=8080
```

启动后，Flyway 会自动运行 `db/migration/` 下的 SQL 脚本创建表结构。

> 看到 `Started DmsApplication` 即表示启动成功。

#### 第四步：导入演示数据（可选）

```bash
# 导入演示数据，方便体验完整功能
mysql -uroot -p openygt_dms < dms-app/src/main/resources/db/demo/init.sql
```

#### 第五步：启动前端

```bash
# 新开一个终端窗口
cd frontend
npm install
npm run dev
```

#### 第六步：访问系统

打开浏览器访问：http://localhost:5173

默认账号：`admin` / `admin123`

> **Windows 用户**：命令中的 `export` 请替换为 `set`

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
├── docker-compose.yml      # Docker 一键部署配置
├── Dockerfile.backend      # 后端镜像构建
└── frontend/Dockerfile.frontend  # 前端镜像构建
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
- **架构设计** → [ARCHITECTURE.md](./ARCHITECTURE.md)

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
