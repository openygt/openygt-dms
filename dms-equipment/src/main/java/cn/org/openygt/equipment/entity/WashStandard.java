package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 清洗标准配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_wash_standard")
public class WashStandard extends BaseEntity {

    /** 1=煎药机 2=包装机 */
    private Integer deviceType;
    /** 1=常规 2=强化 */
    private Integer washType;
    private Integer standardDuration;
    private String washSteps;
    private Integer alertThreshold;
    private Integer isActive;
}
