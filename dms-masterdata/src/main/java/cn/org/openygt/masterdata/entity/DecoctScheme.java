package cn.org.openygt.masterdata.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 煎药方案实体，对应表 {@code md_decoct_scheme}。
 */
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

    // ===== Phase 1 扩展字段 =====
    /** 一煎时间(分钟) */
    private Integer firstDecoctTime;
    /** 二煎时间(分钟) */
    private Integer secondDecoctTime;
    /** 浸泡时间(分钟) */
    private Integer soakTime;
    /** 出液时间(分钟) */
    private Integer drainTime;
    /** 包装时间(分钟) */
    private Integer packageTime;
    /** 后下提醒提前时间(分钟) */
    private Integer lateAddRemindTime;
    /** 升温速率(°C/min) */
    private BigDecimal tempRiseRate;
    /** 是否默认方案 */
    private Integer isDefault;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSchemeType() {
        return schemeType;
    }

    public void setSchemeType(Integer schemeType) {
        this.schemeType = schemeType;
    }

    public Integer getDecoctTimes() {
        return decoctTimes;
    }

    public void setDecoctTimes(Integer decoctTimes) {
        this.decoctTimes = decoctTimes;
    }

    public Integer getPressure() {
        return pressure;
    }

    public void setPressure(Integer pressure) {
        this.pressure = pressure;
    }

    public BigDecimal getUpperWater() {
        return upperWater;
    }

    public void setUpperWater(BigDecimal upperWater) {
        this.upperWater = upperWater;
    }

    public Integer getHeatingTime() {
        return heatingTime;
    }

    public void setHeatingTime(Integer heatingTime) {
        this.heatingTime = heatingTime;
    }

    public Integer getPreHeatingTime() {
        return preHeatingTime;
    }

    public void setPreHeatingTime(Integer preHeatingTime) {
        this.preHeatingTime = preHeatingTime;
    }

    public Integer getPostHeatingTime() {
        return postHeatingTime;
    }

    public void setPostHeatingTime(Integer postHeatingTime) {
        this.postHeatingTime = postHeatingTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAlarmHighTemp() {
        return alarmHighTemp;
    }

    public void setAlarmHighTemp(BigDecimal alarmHighTemp) {
        this.alarmHighTemp = alarmHighTemp;
    }

    public BigDecimal getAlarmLowTemp() {
        return alarmLowTemp;
    }

    public void setAlarmLowTemp(BigDecimal alarmLowTemp) {
        this.alarmLowTemp = alarmLowTemp;
    }

    public Integer getFirstDecoctTime() {
        return firstDecoctTime;
    }

    public void setFirstDecoctTime(Integer firstDecoctTime) {
        this.firstDecoctTime = firstDecoctTime;
    }

    public Integer getSecondDecoctTime() {
        return secondDecoctTime;
    }

    public void setSecondDecoctTime(Integer secondDecoctTime) {
        this.secondDecoctTime = secondDecoctTime;
    }

    public Integer getSoakTime() {
        return soakTime;
    }

    public void setSoakTime(Integer soakTime) {
        this.soakTime = soakTime;
    }

    public Integer getDrainTime() {
        return drainTime;
    }

    public void setDrainTime(Integer drainTime) {
        this.drainTime = drainTime;
    }

    public Integer getPackageTime() {
        return packageTime;
    }

    public void setPackageTime(Integer packageTime) {
        this.packageTime = packageTime;
    }

    public Integer getLateAddRemindTime() {
        return lateAddRemindTime;
    }

    public void setLateAddRemindTime(Integer lateAddRemindTime) {
        this.lateAddRemindTime = lateAddRemindTime;
    }

    public BigDecimal getTempRiseRate() {
        return tempRiseRate;
    }

    public void setTempRiseRate(BigDecimal tempRiseRate) {
        this.tempRiseRate = tempRiseRate;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }
}
