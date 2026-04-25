package cn.org.openygt.analytics;

/**
 * 运营分析模块 (Operations Analytics)
 *
 * 表前缀: ops_
 * API前缀: /api/v1/ops/
 *
 * 职责:
 * - 产能报表
 * - 设备利用率
 * - 人员绩效
 * - 损耗分析
 * - 监控大屏
 */
public final class AnalyticsModule {
    private AnalyticsModule() {}
    public static final String TABLE_PREFIX = "ops_";
    public static final String API_PREFIX = "/api/v1/ops";
}
