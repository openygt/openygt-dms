package cn.org.openygt.masterdata.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 煎药方案创建请求。
 */
public class SchemeCreateRequest {

    @NotBlank(message = "方案名称不能为空")
    private String name;

    @NotBlank(message = "方案编码不能为空")
    private String code;

    @NotNull(message = "煎煮类型不能为空")
    private Integer schemeType;
    private Integer decoctTimes;
    private Integer pressure;
    private BigDecimal upperWater;
    private Integer heatingTime;
    private Integer preHeatingTime;
    private Integer postHeatingTime;
    private String description;

    // Phase 1 扩展字段
    @NotNull(message = "一煎时长不能为空")
    private Integer firstDecoctTime;

    @NotNull(message = "二煎时长不能为空")
    private Integer secondDecoctTime;
    private Integer soakTime;
    private Integer drainTime;
    private Integer packageTime;
    private Integer lateAddRemindTime;
    private BigDecimal tempRiseRate;
    private Integer isDefault;

    /** 高温报警阈值（℃） */
    private BigDecimal alarmHighTemp;

    /** 低温报警阈值（℃） */
    private BigDecimal alarmLowTemp;

    /** 状态: 1启用 0禁用 */
    @NotNull(message = "状态不能为空")
    private Integer status;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
