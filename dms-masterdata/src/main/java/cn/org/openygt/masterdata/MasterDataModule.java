package cn.org.openygt.masterdata;

/**
 * 主数据模块 (Master Data)
 *
 * 表前缀: md_
 * API前缀: /api/v1/md/
 *
 * 职责:
 * - md_hospital: 客户医院
 * - md_decoct_scheme: 煎药方案
 * - 药品字典、处方模板、班组/人员（后续迭代）
 */
public final class MasterDataModule {
    private MasterDataModule() {}
    public static final String TABLE_PREFIX = "md_";
    public static final String API_PREFIX = "/api/v1/md";
}
