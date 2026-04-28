# 单元测试补充计划（N6）

> 对应 REVIEW-2026-04-28 项 N6：dms-print / dms-rbac / dms-quality 关键路径测试补齐
> 目标：各模块测试文件数 ≥ 5 个，覆盖 Controller + Service 核心路径
> 约束：**新增/修改的 Java 测试代码应提交到各模块 `src/test/java`，本计划文档仅存在于 `tests/unit-plans/`**

---

## 一、当前测试分布基线

| 模块 | 现有测试文件数 | 缺口 | 优先级 |
|---|---|---|---|
| dms-system | 10 | 0 | - |
| dms-equipment | 6 | 0 | - |
| dms-masterdata | 6 | 0 | - |
| dms-production | 4 | 1 | P2 |
| dms-print | 2 | 3 | **P1** |
| dms-rbac | 2 | 3 | **P1** |
| dms-quality | 2 | 3 | **P1** |
| dms-analytics | 1 | 4 | P2 |
| dms-inventory | 1 | 4 | P2 |
| dms-common | 1 | 4 | P2 |
| dms-app | 1 | 4 | P2 |

---

## 二、待补充测试清单

### 2.1 dms-print（P1，缺 3 个）

打印模块含模拟执行关键路径，需优先覆盖。

| # | 测试类 | 被测目标 | 核心场景 |
|---|---|---|---|
| 1 | `PrintControllerTest` | `PrintController` | 创建打印任务、查询打印状态 |
| 2 | `PrintServiceImplTest` | `PrintServiceImpl` | 打印任务状态流转、模拟执行成功/失败 |
| 3 | `PrintTaskDTOTest` | `PrintTaskDTO` / 实体 | 字段映射、校验规则 |

### 2.2 dms-rbac（P1，缺 3 个）

权限校验为核心安全路径。

| # | 测试类 | 被测目标 | 核心场景 |
|---|---|---|---|
| 1 | `RbacControllerTest` | `RbacController` | 角色 CRUD、菜单分配（现有，可扩展） |
| 2 | `PermissionInterceptorTest` | `PermissionInterceptor` | 有权限放行、无权限 403（现有，可扩展） |
| 3 | `RoleServiceImplTest` | `RoleServiceImpl` | 角色-权限绑定、缓存刷新 |
| 4 | `MenuServiceImplTest` | `MenuServiceImpl` | 菜单树构建、权限码生成 |
| 5 | `UserRoleServiceImplTest` | `UserRoleServiceImpl` | 用户角色分配、批量更新 |

### 2.3 dms-quality（P1，缺 3 个）

质检模块为生产流程关键节点。

| # | 测试类 | 被测目标 | 核心场景 |
|---|---|---|---|
| 1 | `QualityControllerTest` | `QualityController` | 提交质检结果、查询质检记录（现有，可扩展） |
| 2 | `QualityServiceImplTest` | `QualityServiceImpl` | 质检状态流转、不合格处理（现有，可扩展） |
| 3 | `InspectionTest` | `Inspection` 实体 | 字段校验、枚举映射 |
| 4 | `InspectionResultTest` | `InspectionResult` | 结果判定逻辑（合格/不合格/复检） |
| 5 | `QualitySpiTest` | `QualityService` SPI | 跨模块调用契约 |

### 2.4 dms-production（P2，补 1 个）

| # | 测试类 | 被测目标 | 核心场景 |
|---|---|---|---|
| 1 | `PrescriptionControllerTest` | `PrescriptionController` | 处方创建、关联任务 |

---

## 三、测试规范

1. **框架**：JUnit 5 + Mockito + Spring Boot Test（`@WebMvcTest`、`@SpringBootTest`）
2. **命名**：`*Test.java`，放置于 `src/test/java/.../同包路径`
3. **隔离**：Service 层测试使用 `@ExtendWith(MockitoExtension.class)`，不依赖数据库
4. **覆盖率**：新增测试目标行覆盖率 ≥ 60%
5. **常量**：测试中硬编码字符串需与当前代码一致，如登录路径使用 `/api/v1/rbac/auth/login`

---

## 四、验收 Checklist

- [ ] dms-print 测试文件 ≥ 5 个
- [ ] dms-rbac 测试文件 ≥ 5 个
- [ ] dms-quality 测试文件 ≥ 5 个
- [ ] `mvn test` 全量通过（含新增）
- [ ] JaCoCo 报告（如有）各目标模块行覆盖率 ≥ 40%
