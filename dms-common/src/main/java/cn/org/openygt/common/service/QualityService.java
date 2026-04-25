package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.InspectionResult;

/**
 * 质量追溯模块对外服务接口。
 * 定义在 dms-common，由 dms-quality 实现。
 * dms-production 通过注入此接口提交质检请求。
 */
public interface QualityService {

    /**
     * 对指定任务执行质检。
     *
     * @param taskId     任务 ID
     * @param result     质检结果：通过 / 让步放行 / 返工 / 报废
     * @param operatorId 操作人
     * @param remark     备注
     * @return 质检结果，包含生成的质检记录 ID 及任务下一状态
     */
    InspectionResult inspect(Long taskId, String result, String operatorId, String remark);

    /**
     * 查询任务最新的质检记录。
     *
     * @param taskId 任务 ID
     * @return 质检结果
     */
    InspectionResult getInspectionByTaskId(Long taskId);
}
