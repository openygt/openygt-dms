

# 参与 OpenYGT 共建

感谢您对 OpenYGT 开源医共体感兴趣！无论您是医院信息科工程师、设备厂商技术员、还是独立开发者，都欢迎加入。

---

## 一、快速开始（5分钟上手）

### 1.1 环境准备

```bash
# 克隆仓库
git clone https://gitee.com/openygt/openygt-dms.git
cd openygt-dms

# 一键启动（Docker Compose）
docker compose up -d

# 访问
# Web 管理端：http://localhost:5176
# 默认账号：admin / admin123
```

### 1.2 开发环境

| 组件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Maven | 3.8+ |

---

## 二、我能贡献什么？

### 2.1 医院用户（不用写代码）

- **报 Bug：** 遇到报错截图 + 操作步骤 + 环境信息（浏览器版本、操作系统）
- **提需求：** "我们医院煎药室需要支持双人复核签字"
- **写案例：** 部署后的使用心得、效率提升数据、遇到的问题和解决方式

### 2.2 开发者（写代码）

| 类型 | 示例 | 难度 |
|------|------|------|
| 修 Bug | 煎药任务状态显示不正确 | ⭐ |
| 加功能 | 新增"代煎"业务类型支持 | ⭐⭐ |
| 设备对接 | 适配某品牌煎药机 MQTT 协议 | ⭐⭐⭐ |
| 文档完善 | 补充部署教程、API 文档 | ⭐ |

### 2.3 设备厂商（硬件对接）

- 提供设备通信协议文档（MQTT/串口/HTTP）
- 提供测试环境或模拟器
- 协助调试对接代码

---

## 三、提交贡献的流程

### 3.1 发现问题 → 先查 Issue

1. 打开 [Gitee Issues](https://gitee.com/openygt/openygt-dms/issues)
2. 搜索关键词，确认没人提过
3. 新建 Issue，按模板填写

### 3.2 想改代码 → Fork + PR

```bash
# 1. 在 Gitee 上 Fork 仓库到自己的账号

# 2. 克隆你自己的 Fork
git clone https://gitee.com/你的用户名/openygt-dms.git

# 3. 创建分支（不要直接在 master 上改）
git checkout -b fix/问题简述
# 或
git checkout -b feat/功能名称

# 4. 改代码、测试、提交
git add .
git commit -m "fix: 修复煎药任务状态同步延迟问题"
# 或
git commit -m "feat: 新增包装机异常报警推送"

# 5. 推送到你的 Fork
git push origin fix/问题简述

# 6. 在 Gitee 上发起 Pull Request（PR）
# 目标分支选 openygt/openygt-dms 的 master
# PR 描述里写清楚：改了什么、为什么改、怎么测试的
```

### 3.3 PR 描述模板

```markdown
## 改动内容
- 修复了 XXX 问题
- 新增了 YYY 功能

## 关联 Issue
Fixes #123

## 测试方式
- [ ] 本地 Docker 启动通过
- [ ] 涉及数据库变更的，已提供 migration 脚本
- [ ] 涉及 API 变更的，已更新接口文档

## 截图（如有 UI 改动）
[贴图]
```

---

## 四、代码规范（不复杂）

### 4.1 提交信息格式

```
类型: 简短描述（50字以内）

类型说明：
- feat: 新功能
- fix: 修复 Bug
- docs: 文档更新
- style: 代码格式（不影响功能）
- refactor: 重构
- test: 测试相关
- chore: 构建/工具链
```

示例：
```
feat: 新增煎药机温度异常报警推送
fix: 修复 PDA 扫码后任务状态未更新问题
docs: 补充 Docker 部署常见问题
```

### 4.2 Java 代码风格

- 使用项目根目录的 `checkstyle.xml`（如有）
- 类名 UpperCamelCase，方法名 lowerCamelCase
- 注释用中文，因为用户大多是国内医院

### 4.3 Vue 前端风格

- 组件名大驼峰（如 `TaskBoard.vue`）
- 方法名小驼峰
- 关键逻辑加注释

---

## 五、CLA 签署（必须）

**提交 PR 前，请在 PR 描述中注明：**

> 我同意 OpenYGT CLA（贡献者许可协议）。

这是法律要求，确保我们能把您的改进纳入商业版，同时您仍保留署名权。

CLA 全文见：[CLA.md](./CLA.md)

---

## 六、获取帮助

| 渠道 | 适用场景 |
|------|---------|
| Gitee Issues | Bug 报告、功能建议 |
| 邮件 luobin@openygt.org.cn | 商务合作、设备对接 |
| 社区微信 openygt | 日常交流、提问 |

---

## 七、贡献者荣誉墙

您的贡献会被记录在这里（定期更新）：

| 贡献者 | 贡献类型 | 主要贡献 |
|--------|---------|---------|
| [待填写] | | |

---

再次感谢！每一个 PR、每一个 Bug 报告、每一篇使用心得，都是在帮基层医院的信息化建设铺路。