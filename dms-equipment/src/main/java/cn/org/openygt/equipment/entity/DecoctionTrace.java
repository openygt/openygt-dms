package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("decoction_trace")
public class DecoctionTrace extends BaseEntity {

    private String prescriptionNo;
    private String patientName;
    private String patientPhone;
    private Long taskId;

    // 设备信息
    private String decoctDeviceCode;
    private String decoctDeviceName;
    private String packerDeviceCode;
    private String labelerDeviceCode;

    // 方案信息快照
    private Long schemeId;
    private String schemeName;
    private Integer soakTime;
    private Integer preDecoctTime;
    private Integer firstDecoctTime;
    private Integer addLateTime;
    private Integer secondDecoctTime;
    private Integer packageTime;
    private BigDecimal packageVolume;
    private Integer sampleCount;

    // 时间戳链
    private LocalDateTime receiveTime;
    private LocalDateTime auditTime;
    private LocalDateTime auditPassTime;
    private LocalDateTime dispenseTime;
    private LocalDateTime reviewTime;
    private LocalDateTime soakStartTime;
    private LocalDateTime soakEndTime;
    private LocalDateTime preDecoctStart;
    private LocalDateTime preDecoctEnd;
    private LocalDateTime firstDecoctStart;
    private LocalDateTime firstDecoctEnd;
    private LocalDateTime addLateTimeActual;
    private LocalDateTime secondDecoctStart;
    private LocalDateTime secondDecoctEnd;
    private LocalDateTime packageStartTime;
    private LocalDateTime packageEndTime;
    private LocalDateTime deliverTime;
    private LocalDateTime completeTime;

    // 操作人链
    private String receiveOperator;
    private String auditOperator;
    private String dispenseOperator;
    private String reviewOperator;
    private String soakOperator;
    private String decoctOperator;
    private String packageOperator;
    private String deliverOperator;

    // 温度与质量数据
    private String tempCurveData;
    private BigDecimal maxTemp;
    private BigDecimal avgTemp;
    private String waterQualityCheck;

    // 状态与异常
    private String status;
    private String exceptionReason;
    private String exceptionHandleResult;

    // 打印与交付
    private Integer labelPrintCount;
    private String deliveryNo;
    private String deliveryCompany;
}
