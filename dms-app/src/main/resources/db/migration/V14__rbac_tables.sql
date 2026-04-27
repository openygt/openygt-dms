-- V14: RBAC 权限表（V2.0 固定 5 角色，无数据权限）

CREATE TABLE IF NOT EXISTS sys_menu (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(50) DEFAULT 'default',
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    path VARCHAR(200),
    component VARCHAR(200),
    icon VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    menu_type INTEGER DEFAULT 1,
    parent_id INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    permission VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id VARCHAR(50) DEFAULT 'default',
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    sort_order INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_id INTEGER NOT NULL,
    menu_id INTEGER NOT NULL,
    UNIQUE(role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    UNIQUE(user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_menu_parent ON sys_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_menu_status ON sys_menu(status);
CREATE INDEX IF NOT EXISTS idx_sys_role_code ON sys_role(role_code);
CREATE INDEX IF NOT EXISTS idx_sys_role_status ON sys_role(status);
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_role ON sys_role_menu(role_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_menu ON sys_role_menu(menu_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_user ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role ON sys_user_role(role_id);

-- 初始化 5 个固定角色
INSERT OR IGNORE INTO sys_role (role_code, role_name, description, sort_order) VALUES
('ROLE_ADMIN',     '系统管理员', '拥有全部权限', 1),
('ROLE_DIRECTOR',  '车间主任',   '管理车间生产、人员、设备', 2),
('ROLE_LEADER',    '班组长',     '管理班组任务与人员', 3),
('ROLE_WORKER',    '操作工',     '执行煎煮、投料等操作', 4),
('ROLE_INSPECTOR', '质检员',     '执行质量检验与放行', 5);

-- 初始化基础菜单
INSERT OR IGNORE INTO sys_menu (name, code, path, component, icon, sort_order, menu_type, parent_id, permission) VALUES
('系统管理',  'system',       '/system',       NULL,         'SettingOutlined',   1, 0, 0, NULL),
('用户管理',  'system_user',  '/system/user',  'system/user', 'UserOutlined',      1, 1, 1, NULL),
('角色管理',  'system_role',  '/system/role',  'system/role', 'TeamOutlined',      2, 1, 1, NULL),
('菜单管理',  'system_menu',  '/system/menu',  'system/menu', 'MenuOutlined',      3, 1, 1, NULL),
('生产管理',  'production',   '/production',   NULL,          'ToolOutlined',      2, 0, 0, NULL),
('煎煮任务',  'prod_task',    '/production/task', 'production/task', 'FireOutlined', 1, 1, 5, NULL),
('设备管理',  'prod_device',  '/production/device', 'production/device', 'MonitorOutlined', 2, 1, 5, NULL),
('质量管理',  'quality',      '/quality',      NULL,          'SafetyOutlined',    3, 0, 0, NULL),
('质量检验',  'qa_inspect',   '/quality/inspect', 'quality/inspect', 'CheckCircleOutlined', 1, 1, 8, NULL),
('数据分析',  'analytics',    '/analytics',    NULL,          'BarChartOutlined',  4, 0, 0, NULL),
('PDA作业',   'pda',          '/pda',          NULL,          'MobileOutlined',    5, 0, 0, NULL);

-- 为管理员角色绑定所有菜单
INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m WHERE r.role_code = 'ROLE_ADMIN';

-- 为车间主任绑定生产+质量+系统查看菜单
INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_DIRECTOR' AND m.code IN ('production', 'prod_task', 'prod_device', 'quality', 'qa_inspect', 'analytics', 'system');

-- 为班组长绑定生产任务菜单
INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_LEADER' AND m.code IN ('production', 'prod_task', 'pda');

-- 为操作工绑定 PDA 和生产任务
INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_WORKER' AND m.code IN ('pda', 'production', 'prod_task');

-- 为质检员绑定质量菜单
INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'ROLE_INSPECTOR' AND m.code IN ('quality', 'qa_inspect', 'pda');

-- 预置 admin → ROLE_ADMIN 绑定（确保管理员登录后有角色权限）
INSERT OR IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_ADMIN';
