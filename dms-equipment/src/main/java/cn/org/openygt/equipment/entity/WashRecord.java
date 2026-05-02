package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 设备清洗记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_wash_record")
public class WashRecord extends BaseEntity {

    private Long deviceId;
    private String deviceCode;
    private Long taskId;
    private Long prescriptionId;
    /** 1=常规清洗 2=强化清洗 */
    private Integer washType;
    private Integer standardDuration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMin;
    /** 1=合格 0=不合格 */
    private Integer result;
    private Long operatorId;
    private String operatorName;
    private String remark;
}
