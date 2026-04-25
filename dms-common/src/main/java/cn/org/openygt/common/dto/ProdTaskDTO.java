package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 生产任务查询传输对象。
 * ProductionQueryService 的返回类型，供 analytics/quality/print 反查任务信息。
 */
@Data
public class ProdTaskDTO implements Serializable {

    private Long id;
    private Long prescriptionId;
    private String status;
    private Long decoctDeviceId;
    private Long packageDeviceId;
    private String operatorId;
    private Integer isException;
    private String exceptionReason;
    private LocalDateTime handoverTime;
    private LocalDateTime completeTime;
    private LocalDateTime createdAt;
}
