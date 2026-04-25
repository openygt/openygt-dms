package cn.org.openygt.common.dto;

import lombok.Data;

import java.util.Date;

/**
 * 质检操作结果。
 */
@Data
public class InspectionResult {

    private Long inspectionId;
    private Long taskId;
    private String result;
    private String nextStatus;
    private String operatorId;
    private String remark;
    private Date inspectedAt;
    private Integer isException;
    private String exceptionReason;
}
