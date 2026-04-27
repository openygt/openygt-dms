# Master 分支前端全面测试报告

> **被测分支**: `master`  
> **测试基线 Commit**: `e25194aa feat(frontend+backend+test): Dashboard看板对接 + 后端单元测试补充`  
> **测试人**: 工程师A（测试）  
> **测试时间**: 2026-04-27 20:12~20:30  
> **测试环境**: SQLite 本地模式 / localhost:8080 + 前端 Vite dev server (5176)

---

## 1. 结果摘要

| 类别 | 检查项 | 结果 | 说明 |
|------|--------|------|------|
| 前端构建 | `npm run build` | ✅ 通过 | 11.76s，0 错误，17 个页面全部打包 |
| 文件完整性 | 视图/布局/路由文件 | ✅ 18/18 | 所有组件文件存在 |
| 路由配置 | 16 条路由 + 权限守卫 | ✅ 通过 | beforeEach 拦截登录与权限 |
| API 路径一致性 | 前后端路径对比 | ⚠️ 部分差异 | 3 个 404，2 个 500 |
| 后端联调 | 核心链路 | ⚠️ 有条件通过 | 13/16 接口 200 |

---

## 2. 前端静态检查

### 2.1 构建验证 ✅

```bash
cd frontend && npm run build
```

- 构建时间: 11.76s
- 错误数: 0
- 警告: 1 个（index.js 1.2MB，Element Plus 正常体积）
- 输出页面: 17 个（含 Login、Dashboard、Tasks、Prescriptions、ConsumeLog、Devices、Alarms、PrintCenter、Quality、Capacity、Users、Roles、Menus、Configs、Logs、Hospitals、Schemes）

### 2.2 文件完整性 ✅

| 文件 | 状态 |
|------|------|
| `src/App.vue` | ✅ |
| `src/main.ts` | ✅ |
| `src/router/index.ts` | ✅ |
| `src/api/request.ts` | ✅ |
| `src/stores/user.ts` | ✅ |
| `src/components/Layout.vue` | ✅ |
| `src/views/LoginView.vue` | ✅ |
| `src/views/DashboardView.vue` | ✅ |
| `src/views/task/TaskListView.vue` | ✅ |
| `src/views/prod/PrescriptionListView.vue` | ✅ |
| `src/views/inventory/ConsumeLogView.vue` | ✅ |
| `src/views/eq/DeviceListView.vue` | ✅ |
| `src/views/monitor/AlarmLogView.vue` | ✅ |
| `src/views/prt/PrintCenterView.vue` | ✅ |
| `src/views/qt/QualityListView.vue` | ✅ |
| `src/views/ops/CapacityReportView.vue` | ✅ |
| `src/views/system/*` (5个) | ✅ |
| `src/views/md/*` (2个) | ✅ |

### 2.3 技术栈检查 ✅

- Vue 3.4.21 + Vite 5.2.8
- Vue Router 4.3.0（history 模式）
- Pinia 2.1.7（状态管理）
- Element Plus 2.6.3 + Icons
- Axios（baseURL: `/api`，timeout: 30s）
- Vite 代理: `/api` → `http://localhost:8080`

### 2.4 路由与权限 ✅

- 登录拦截: 未登录自动跳转 `/login`
- 权限拦截: `meta.perm` 与 `userStore.hasPermission()` 联动
- Token 存储: localStorage（持久化）
- 菜单获取: 登录后自动拉取 `/api/v1/rbac/menus/tree`

---

## 3. 前后端 API 路径一致性检查

### 3.1 路径匹配 ✅

| 前端调用 | 后端路径 | 结果 |
|----------|----------|------|
| `/api/v1/rbac/auth/login` | `RbacController.loginWithRoles()` | ✅ 200 |
| `/api/v1/rbac/menus/tree` | `SysMenuController.getMenuTree()` | ✅ 200 |
| `/api/v1/rbac/roles` | `RbacController.roleList()` | ✅ 200 |
| `/api/v1/eq/devices` | `EqDeviceController` | ✅ 200 |
| `/api/v1/prod/prescriptions` | `PrescriptionController` | ✅ 200 |
| `/api/v1/prod/tasks` | `TaskController` | ✅ 200 |
| `/api/v1/eq/alarms` | `EqAlarmController` | ✅ 200 |
| `/api/v1/ops/capacity/daily` | `CapacityController` | ✅ 200 |
| `/api/v1/sys/configs` | `SysConfigController` | ✅ 200 |
| `/api/v1/sys/users` | `SysUserController` | ✅ 200 |
| `/api/v1/md/hospitals` | `HospitalController` | ✅ 200 |
| `/api/v1/md/schemes` | `DecoctSchemeController` | ✅ 200 |
| `/api/v1/ops/dashboard/realtime` | `DashboardController.realtime()` | ✅ 200 |

### 3.2 路径不匹配 / 缺失 ❌

| 前端调用 | 后端现状 | 问题 | 严重程度 |
|----------|----------|------|----------|
| `/api/inventory/consume/list` | `ConsumeRecordController.pageQuery()` | **500** — master 缺少 `inv_stock_log` 表（V20 未合并），MyBatis 查询报错 | P0 |
| `/api/v1/sys/logs` | `SysLogController` | **500** — `sys_log` 表缺少 `updated_at` 字段（V4 创建时未包含，但 `BaseAuditEntity` 需要） | P1 |
| `/api/v1/qt/inspection/0` | `QualityController.getInspection()` | **500** — 需要进一步排查，可能表结构或字段不匹配 | P1 |
| `/api/v1/prt/tasks?page=1&size=5` | `PrintController` | **404** — 后端仅有 `/tasks/{taskId}/submit` 和 `/tasks/{taskId}/retry`，无列表查询接口 | P1 |

---

## 4. 数据库问题（阻塞前端功能）

### 4.1 V5 脚本重复列（已修复）

- **问题**: `V5__v1_4_refactor.sql` 中 `ALTER TABLE qt_inspection ADD COLUMN inspected_at DATETIME` 与 V4 重复，导致空库启动失败。
- **修复**: 已注释该 ALTER（master 分支本地修改）。

### 4.2 sys_log 缺少 updated_at ⚠️

- **表结构** (V4): `sys_log` 无 `updated_at`
- **实体类**: `SysLog extends BaseAuditEntity`，含 `updatedAt` 字段
- **后果**: MyBatis-Plus 查询时 SELECT 包含 `updated_at`，SQLite 报错 → 500
- **建议**: 在 V5 或新增 V15 中 `ALTER TABLE sys_log ADD COLUMN updated_at DATETIME`

### 4.3 inv_stock_log 表缺失 ⚠️

- **问题**: `ConsumeRecordServiceImpl` 依赖 `inv_stock_log`，但 master 分支无该表（在 feat/task-consume-optional 的 V20 中）
- **后果**: 消耗流水页面无法加载数据
- **建议**: 将 V20 中 `inv_stock_log` 相关 DDL 合并到 master，或在 master 新增独立迁移脚本

---

## 5. 代码变更（测试侧本地修复）

| 文件 | 修改 | 说明 |
|------|------|------|
| `dms-app/src/main/resources/db/migration/V5__v1_4_refactor.sql` | 注释重复 ALTER | 修复空库启动阻塞 |

---

## 6. 结论与建议

### 6.1 前端质量

- ✅ **构建稳定**: 无编译错误，17 页面完整输出
- ✅ **路由权限**: 登录拦截、权限守卫、菜单动态加载正常
- ✅ **API 代理**: Vite dev server 代理配置正确
- ⚠️ **接口依赖**: 3 个后端接口 500/404，导致对应前端页面无法正常使用

### 6.2 合流建议

master 分支当前状态：

1. **可独立运行**: 后端可启动，前端可构建，基础功能（登录、看板、任务、设备、处方）正常
2. **阻塞功能**: 消耗流水、系统日志、质检记录、打印中心列表 不可用
3. **必须先修后合流**:
   - 方案A：将 feat/task-consume-optional 的 V20 数据库脚本合并到 master
   - 方案B：在 master 单独创建 V15 迁移脚本，补齐 inv_stock_log + sys_log.updated_at
   - 方案C：后端接口增加空数据兜底，避免 500

**建议优先级**: P0 修复数据库表缺失 → P1 补齐缺失列表接口 → 然后合流到 integration。
