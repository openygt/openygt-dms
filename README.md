# OpenYGT-DMS 智能煎药管理系统

[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/docker--compose-ready-2496ED.svg)]()

面向中药房汤剂煎煮场景的全流程智能管理系统，覆盖从处方接收到患者取药的完整链路。

---

## 快速开始

```bash
# 一键启动（Docker Compose）
git clone https://gitee.com/openygt/openygt-dms.git
cd openygt-dms
docker compose up -d

# 访问地址
# Web 管理端：http://localhost:5176
# 默认账号：admin / admin123
```

## 技术栈

| 层级 | 技术选型 |
|------|----------|
| 前端（Web） | Vue 3 + TypeScript + Element Plus |
| 前端（PDA） | uni-app（Vue 3），支持 H5、Android PDA 设备 |
| 后端 | Spring Boot 3 + MyBatis-Plus |
| 数据库 | MySQL 8.0（信创适配达梦 DM8 进行中） |
| 消息 | MQTT（设备通信） |
| 部署 | Docker Compose 一键部署 |

---

## 文档与帮助

���� **��完整文档已迁移至独立仓库：**
- 用户手册、部署指南、开发文档、设备对接指南 → [openygt-docs](https://gitee.com/openygt/openygt-docs)
- 在线阅读 → https://openygt.org.cn/docs

���� **��技术交流**
- Gitee Issues（推荐）
- 社区微信：openygt

���� **��商务合作**
- 商业授权 / 私有化部署 / 定制开发：luobin@openygt.org.cn

---

## 开源协议

OpenYGT-DMS 采用 **双协议模式**：

| 版本 | 协议 | 适用场景 |
|------|------|----------|
| 开源版 | [AGPL-3.0](LICENSE) | 免费用，SaaS/云端部署需开源修改代码 |
| 商业版 | [商业授权协议](LICENSE.commercial.md) | 医院闭源部署、豁免开源义务 |

> 如需商业授权，请联系：luobin@openygt.org.cn

---

<p align="center">
  <sub>Copyright © 2026 OpenYGT Contributors & 上海周方智能科技有限公司</sub>
</p>
