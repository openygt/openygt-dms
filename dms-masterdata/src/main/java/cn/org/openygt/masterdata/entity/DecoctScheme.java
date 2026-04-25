package cn.org.openygt.masterdata.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 煎药方案实体，对应表 {@code md_decoct_scheme}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_decoct_scheme")
public class DecoctScheme extends BaseEntity {

    private String code;
    private String name;
    private Integer schemeType;
    private Integer decoctTimes;
    private Integer pressure;
    private BigDecimal upperWater;
    private Integer heatingTime;
    private Integer preHeatingTime;
    private Integer postHeatingTime;
    private String description;

    /** 方案级高温报警阈值（℃），覆盖设备级和设备默认值 */
    private BigDecimal alarmHighTemp;

    /** 方案级低温报警阈值（℃），覆盖设备级和设备默认值 */
    private BigDecimal alarmLowTemp;
}
