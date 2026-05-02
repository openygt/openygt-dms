package cn.org.openygt.masterdata.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 毒性药材清单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_toxic_medicine")
public class ToxicMedicine extends BaseEntity {

    private Long medicineId;
    private String medicineName;
    /** 1小毒 2有毒 3大毒 */
    private Integer toxicityLevel;
    private BigDecimal maxDosage;
    private BigDecimal maxDailyDosage;
    /** 1常规 2强化 */
    private Integer washLevel;
    private Integer isActive;
    private String remark;
}
