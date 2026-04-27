# 工程师A 修复回归报告

> **被测 Commit**: `d5322c7b fix(v2/rbac): 修复测试报告 BUG-A-01 / BUG-A-02 / NOTE-01`  
> **基线 Commit**: `25f20b26 feat(v2): RBAC 固定5角色权限模块（无数据权限）`  
> **测试人**: 工程师F  
> **回归时间**: 2026-04-27 14:55  
> **结论**: ✅ **RBAC 模块通过，允许合流**

---

## 1. 回归范围

| Bug | 描述 | 修复内容 |
|-----|------|----------|
| BUG-A-01 | 角色列表接口未做权限保护 | `roleList()` 添加 `@RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})` |
| BUG-A-02 | 分配菜单权限接口 500 | 新增 `SysRoleMenuService/Impl`，补全 `assignMenus()` 删旧插新逻辑 |
| NOTE-01 | admin 用户未预置角色绑定 | `V14__rbac_tables.sql` 追加 `admin → ROLE_ADMIN` 预置绑定 |

---

## 2. 验证结果

### 2.1 API 冒烟测试（RBAC 相关）

| 编号 | 场景 | 结果 | 说明 |
|------|------|------|------|
| SMK-AUTH-01 | 正常登录 | ✅ PASS | Token 含 `roles=['ROLE_ADMIN']` |
| SMK-AUTH-02 | 密码错误拦截 | ✅ PASS | 返回非 200 |
| SMK-AUTH-03 | 无 Token 访问受保护接口 | ✅ PASS | 返回 401 |
| SMK-AUTH-04 | 低权限用户访问角色列表被拦截 | ✅ PASS | 煎药工访问 `/api/v1/rbac/roles` 返回 403 |
| SMK-RBAC-01 | 固定 5 角色完整 | ✅ PASS | 5 个预置角色全部返回 |
| SMK-RBAC-03 | 分配菜单权限成功 | ✅ PASS | `POST /api/v1/rbac/roles/{id}/menus` 返回 200 |
| SMK-ALM-05 | 无语音拨号路径 | ✅ PASS | 白盒扫描无异常 |

**RBAC 通过率: 7/7 ✅**

### 2.2 后端单元测试

```text
cn.org.openygt.rbac.interceptor.PermissionInterceptorTest
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**单测通过率: 6/6 ✅**

---

## 3. 剩余失败说明（非工程师A问题）

| 编号 | 接口 | 状态 | 原因 |
|------|------|------|------|
| SMK-PROD-01 | `POST /api/prescriptions` | 404 | 工程师B 未提交 |
| SMK-INV-01 | `POST /api/inventory/consume/record` | 404 | 工程师B 未提交 |
| SMK-EQ-01 | `GET /api/devices` | 404 | 路径差异，需后续确认 |
| SMK-ALM-01 | `GET /api/v1/monitor/alarms` | 404 | 工程师C 未提交 |

---

## 4. 接口契约差异记录

| 接口 | DDD 文档定义 | 实际实现 | 影响 |
|------|-------------|----------|------|
| `POST /api/v1/rbac/roles/{id}/menus` | `{"menuIds": [20,21,28]}` | 直接接收 JSON 数组 `[20,21,28]` | 冒烟脚本已适配，前后端需对齐约定 |

---

## 5. 合流建议

工程师A 的 `feat/rbac-5-roles-no-datascope` 分支当前状态：

- ✅ RBAC 核心链路全绿
- ✅ 权限拦截（401/403）正常
- ✅ 5 角色预置数据正确
- ✅ 菜单授权功能正常
- ✅ 单测通过

**建议: 允许合并到 `integration` 分支。**

合并前请确认：
1. `V14__rbac_tables.sql` 中的 `COMMENT` 语法问题（NOTE-02）是否已在代码中同步移除（本地测试时已手动移除）；
2. 前后端就 `assignMenus` 请求体格式（对象 vs 数组）达成一致。
