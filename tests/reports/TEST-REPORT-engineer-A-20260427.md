# 工程师A 代码测试报告

> **被测分支**: `feat/rbac-5-roles-no-datascope`（已合入 test/v2-enhancement-regression）  
> **被测 Commit**: `25f20b26 feat(v2): RBAC 固定5角色权限模块（无数据权限）`  
> **测试人**: 工程师F  
> **测试时间**: 2026-04-27 14:35~14:40  
> **测试环境**: SQLite 本地模式 / localhost:8080

---

## 1. 结果摘要

| 类别 | 用例数 | 通过 | 失败 | 阻塞 | 结论 |
|------|--------|------|------|------|------|
| API 冒烟 | 6（RBAC相关） | 4 | 2 | 0 | **有条件通过** |
| 后端单测 | 6 | 6 | 0 | 0 | **通过** |
| **合计** | **12** | **10** | **2** | **0** | |

---

## 2. 通过的测试

| 编号 | 场景 | 说明 |
|------|------|------|
| SMK-AUTH-01 | 正常登录 | `/api/v1/rbac/auth/login` 返回 200，Token 内含 `roles=['ROLE_ADMIN']` |
| SMK-AUTH-02 | 密码错误拦截 | 返回非 200，登录被拦截 |
| SMK-AUTH-03 | 无 Token 访问受保护接口 | 返回 401 |
| SMK-RBAC-01 | 固定 5 角色 | 数据库恰好返回 5 个预置角色：`ROLE_ADMIN/DIRECTOR/LEADER/WORKER/INSPECTOR` |
| PermissionInterceptorTest | 单测 | 6 个用例全部通过，覆盖：无注解放行、有角色通过、无角色拒绝、多角色匹配、角色不匹配、空角色 |

---

## 3. 失败的测试（需工程师A修复）

### BUG-A-01 🐛 SMK-AUTH-04：角色列表接口未做权限保护

| 项 | 内容 |
|---|---|
| 用例 | 低权限用户（煎药工）不应能访问角色列表 |
| 实际 | `GET /api/v1/rbac/roles` 返回 200，任何登录用户均可查看全部角色 |
| 根因 | `RbacController.roleList()` 方法未加 `@RequiresPermissions` 注解 |
| 建议 | 添加 `@RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})` 或至少 `@RequiresPermissions({"ROLE_ADMIN"})` |
| 优先级 | P1 |

### BUG-A-02 🐛 SMK-RBAC-03：分配菜单权限接口 500

| 项 | 内容 |
|---|---|
| 用例 | `POST /api/v1/rbac/roles/{roleId}/menus` 应成功绑定菜单 |
| 实际 | 返回 500 `"服务器内部错误"` |
| 根因 | `assignMenus()` 仅校验角色存在性，未实际调用 `sys_role_menu` 写入逻辑，可能抛出 NPE 或其他异常 |
| 建议 | 补全 `SysRoleMenuService` 绑定逻辑：先删旧关联，再批量插入新关联 |
| 优先级 | P0 |

---

## 4. 测试辅助发现（非阻塞）

| 编号 | 问题 | 说明 | 建议 |
|------|------|------|------|
| NOTE-01 | admin 用户未预置角色绑定 | `sys_user_role` 表初始化时空表，admin 登录后 Token 中 roles 为空，导致 `@RequiresPermissions` 校验全部失败 | 在 `DatabaseInitConfig` 或迁移脚本中预置 `admin → ROLE_ADMIN` 绑定 |
| NOTE-02 | V14 SQL 含 SQLite 不兼容语法 | `V14__rbac_tables.sql` 中 `COMMENT` 子句导致 SQLite 启动失败 | 移除 `COMMENT`（SQLite 不支持），或用注释替代 |
| NOTE-03 | 5 角色编码与任务文档不一致 | 代码中使用 `ROLE_ADMIN/ROLE_DIRECTOR/ROLE_LEADER/ROLE_WORKER/ROLE_INSPECTOR`，TASK 文档中描述为 `admin/director/leader/decocter/inspector` | 确认前后端约定一致即可，当前编码自洽 |

---

## 5. 其他工程师接口状态（非A的问题）

| 编号 | 接口 | 状态 | 原因 |
|------|------|------|------|
| SMK-PROD-01 | `POST /api/prescriptions` | 404 | 工程师B 未提交 |
| SMK-INV-01 | `POST /api/inventory/consume/record` | 404 | 工程师B 未提交 |
| SMK-EQ-01 | `GET /api/devices` | 404 | 路径差异，需确认 |
| SMK-ALM-01 | `GET /api/v1/monitor/alarms` | 404 | 工程师C 未提交 |

---

## 6. 复现命令

```bash
# 1. 启动服务
cd /data2/docker/decoction/openygt-dms
java -jar dms-app/target/dms-app-1.0.0-SNAPSHOT.jar

# 2. 执行 RBAC 冒烟测试
export DMS_BASE_URL=http://localhost:8080
export DMS_USERNAME=admin
export DMS_PASSWORD=admin123
python3 tests/regression/api_smoke_test.py

# 3. 执行 RBAC 单元测试
mvn test -pl dms-rbac
```

---

## 7. 修复后回归建议

工程师A 修复 BUG-A-01、BUG-A-02 及 NOTE-01、NOTE-02 后，请在本地执行：

```bash
mvn clean package -DskipTests
bash tests/regression/run_local_ci.sh
```

全绿后方可发起 PR 到 `integration`。
