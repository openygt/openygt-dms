# system 模块验收检查清单（测试工程师）

## 前置条件检查
- [ ] 开发人员代码已提交到主分支
- [ ] Flyway 脚本已入库
- [ ] 开发工程师自测报告已提交
- [x] 数据库已备份（backup_before_system_20260508_182232.sql）
- [x] 前后端服务正常运行

## RENAME 验证
- [x] sys_data_migration_log 存在，数据量=24
- [x] sys_exc_template 存在，数据量=0
- [x] sys_employee_skill 存在，数据量=0
- [ ] 旧表保留检查（data_migration_log/exc_template/dms_employee_skill）→ 未保留，已记录

## 接口冒烟（curl）
- [x] 用户管理 /api/v1/sys/users → 200
- [x] 角色权限 /api/v1/rbac/roles → 200
- [x] 角色权限 /api/v1/rbac/menus/tree → 200
- [x] 参数配置 /api/v1/sys/configs → 200
- [x] 日志管理 /api/v1/sys/logs → 200
- [x] 系统管理 /api/v1/sys/signature → 200（需参数）

## 数据匹配度
- [x] sys_users 接口返回 = sys_user 表数据量
- [x] sys_roles 接口返回 = sys_role 表数据量
- [x] sys_menus 接口返回 ≈ sys_menu 表数据量
- [ ] sys_logs 接口返回 = sys_log 表数据量 → 不匹配，待开发确认
- [x] sys_configs 接口返回 = sys_config 表数据量

## 页面点测（E2E）
- [ ] 系统管理页面可打开
- [ ] 用户管理页面可打开
- [ ] 角色权限页面可打开
- [ ] 参数配置页面可打开
- [ ] 日志管理页面可打开

## 模块隔离合规
- [ ] system 模块代码只查询 sys_* 表
- [ ] 无跨模块直接查其他前缀表

## 阻塞项
1. 日志接口数据不匹配（38,923 vs sys_log=12）
2. 旧表未保留（违反执行单规则）
