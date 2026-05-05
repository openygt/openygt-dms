package cn.org.openygt.common.service;

/**
 * 质检放行后创建留样（由 dms-quality 实现，生产模块通过 ObjectProvider 可选注入）。
 */
public interface RetainSampleFacade {

    /**
     * PASS / CONCESSION 时确保存在一条 7 天留样（幂等：任务已有未销毁留样则跳过）。
     */
    void createAfterPass(Long taskId, Long prescriptionId, String operatorId);
}
