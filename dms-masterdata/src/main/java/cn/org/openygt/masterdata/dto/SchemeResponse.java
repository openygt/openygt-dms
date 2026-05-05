package cn.org.openygt.masterdata.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 煎药方案响应 DTO（屏蔽 deleted / tenantId）。
 */
public class SchemeResponse {

    private Long id;
    private String schemeName;
    private String schemeCode;
    private Integer schemeType;
    private Integer decoctTimes;
    private Integer pressure;
    private BigDecimal upperWater;
    private Integer decoctTime;
    private Integer soakTime;
    private Integer postHeatingTime;
    private String tempRange;
    private String remark;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Phase 1 扩展字段
    private Integer firstDecoctTime;
    private Integer secondDecoctTime;
    private Integer drainTime;
    private Integer packageTime;
    private Integer lateAddRemindTime;
    private BigDecimal tempRiseRate;
    private Integer isDefault;

    /** 高温报警阈值（℃） */
    private BigDecimal alarmHighTemp;

    /** 低温报警阈值（℃） */
    private BigDecimal alarmLowTemp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

    public String getSchemeCode() {
        return schemeCode;
    }

    public void setSchemeCode(String schemeCode) {
        this.schemeCode = schemeCode;
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

    public Integer getDecoctTime() {
        return decoctTime;
    }

    public void setDecoctTime(Integer decoctTime) {
        this.decoctTime = decoctTime;
    }

    public Integer getSoakTime() {
        return soakTime;
    }

    public void setSoakTime(Integer soakTime) {
        this.soakTime = soakTime;
    }

    public Integer getPostHeatingTime() {
        return postHeatingTime;
    }

    public void setPostHeatingTime(Integer postHeatingTime) {
        this.postHeatingTime = postHeatingTime;
    }

    public String getTempRange() {
        return tempRange;
    }

    public void setTempRange(String tempRange) {
        this.tempRange = tempRange;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
}
