package cn.org.openygt.quality;

/**
 * 质量追溯模块 (Quality & Traceability)
 *
 * 表前缀: qt_
 * API前缀: /api/v1/qt/
 *
 * 职责:
 * - qt_inspection: 质检记录
 * - 返工/报废/让步放行
 * - 追溯查询
 * - 清场记录（后续迭代）
 */
public final class QualityModule {
    private QualityModule() {}
    public static final String TABLE_PREFIX = "qt_";
    public static final String API_PREFIX = "/api/v1/qt";
}
