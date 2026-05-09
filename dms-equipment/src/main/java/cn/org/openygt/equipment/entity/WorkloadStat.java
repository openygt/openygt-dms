package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ops_workload_stat")
public class WorkloadStat extends BaseEntity {

    private LocalDate statDate;
    private Long operatorId;
    private String operatorName;
    private String workType;
    private Integer taskCount;
    private Integer prescriptionCount;
    private Integer packageCount;
    private Integer durationMinutes;
    private BigDecimal efficiency;
}
