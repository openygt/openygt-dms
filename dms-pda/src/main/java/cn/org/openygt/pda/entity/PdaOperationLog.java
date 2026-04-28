package cn.org.openygt.pda.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pda_operation_log")
public class PdaOperationLog extends cn.org.openygt.common.entity.BaseEntity {

    private Long userId;
    private String userName;
    private Long deviceId;
    private String deviceCode;
    private Long taskId;
    private Long prescriptionId;
    private String operType;
    private String operDesc;
    private String extData;
    private String operResult;
    private String errorMsg;
    private String apiPath;
    private String httpMethod;
    private String clientIp;
    private Long requestTime;
    private LocalDateTime operTime;
}
