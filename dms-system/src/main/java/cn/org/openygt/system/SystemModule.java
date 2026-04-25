package cn.org.openygt.system;

/**
 * 系统管理模块 (System Admin)
 *
 * 表前缀: sys_
 * API前缀: /api/v1/sys/
 *
 * 职责:
 * - sys_user: 用户管理
 * - sys_config: 系统配置
 * - sys_log: 操作日志
 * - RBAC权限（后续迭代）
 */
public final class SystemModule {
    private SystemModule() {}
    public static final String TABLE_PREFIX = "sys_";
    public static final String API_PREFIX = "/api/v1/sys";
}
