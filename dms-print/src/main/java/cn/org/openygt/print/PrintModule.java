package cn.org.openygt.print;

/**
 * 打印中心模块 (Print Center)
 *
 * 表前缀: prt_
 * API前缀: /api/v1/prt/
 *
 * 职责:
 * - prt_task: 打印任务队列
 * - prt_record: 打印记录
 * - 模板管理（后续迭代）
 * - 重打/补打
 */
public final class PrintModule {
    private PrintModule() {}
    public static final String TABLE_PREFIX = "prt_";
    public static final String API_PREFIX = "/api/v1/prt";
}
