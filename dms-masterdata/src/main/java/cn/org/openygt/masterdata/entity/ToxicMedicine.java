package cn.org.openygt.masterdata.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 毒性药材清单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_toxic_medicine")
public class ToxicMedicine extends BaseEntity {

    @NotNull(message = "关联药材ID不能为空")
    private Long medicineId;

    @NotBlank(message = "药材名称不能为空")
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
