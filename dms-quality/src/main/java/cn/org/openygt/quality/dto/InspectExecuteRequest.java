package cn.org.openygt.quality.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 质检执行请求（含详细检查项）。
 */
@Data
public class InspectExecuteRequest {

    @NotNull
    private Long taskId;

    private String operatorId;

    /** 总结果：PASS / CONCESSION / REWORK / SCRAP */
    private String overallResult;

    private String remark;

    /** 返工节点（REWORK时必填） */
    private String reworkNode;

    /** 检查项明细 */
    private List<InspectItemDTO> items;

    @Data
    public static class InspectItemDTO {
        private String itemCode;
        private String itemName;
        private String result;
        private String actualValue;
        private String remark;
    }
}
