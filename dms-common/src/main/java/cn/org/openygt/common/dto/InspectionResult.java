package cn.org.openygt.common.dto;

import cn.org.openygt.common.enums.InspectionResultType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 质检操作结果。
 *
 * <p>注：nextStatus 当前为中文状态值（与 prod_task.status 存储格式一致）。
 * V6 迭代将统一迁移为 TaskStatus 枚举名。</p>
 */
@Data
public class InspectionResult {

    private Long inspectionId;
    private Long taskId;
    private InspectionResultType result;
    /** 下一状态（当前为中文标签，V6 改为枚举名） */
    private String nextStatus;
    private String operatorId;
    private String remark;
    private LocalDateTime inspectedAt;
    private Integer isException;
    private String exceptionReason;
}
