package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印任务传输对象。
 * PrintService.getPrintQueue() 的统一返回元素类型。
 */
@Data
public class PrintTaskDTO implements Serializable {

    private Long id;
    private Long taskId;
    private String deviceCode;
    private String operatorId;
    private String status;
    private Integer copies;
    private Integer retryCount;
    private Integer maxRetry;
    private String printType;
    private LocalDateTime createdAt;
}
