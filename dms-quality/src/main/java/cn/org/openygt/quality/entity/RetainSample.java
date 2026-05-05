package cn.org.openygt.quality.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 留样记录表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qt_retain_sample")
public class RetainSample extends BaseEntity {

    private Long taskId;
    private Long prescriptionId;
    private String sampleNo;
    /** 固定 7 天留样，恒为 4（库字段保留以兼容历史脚本） */
    private Integer sampleType;
    private Integer retainDuration;
    private LocalDateTime retainTime;
    private LocalDateTime expireTime;
    /** 1=留样中 2=已复检 3=可销毁 4=已销毁 */
    private Integer status;
    private LocalDateTime destroyTime;
    private Long destroyBy;
    private Long operatorId;
    private String remark;
}
