package cn.org.openygt.equipment.dto;

import java.math.BigDecimal;

/**
 * MQTT 状态上报消息 JSON 解析 DTO。
 *
 * <p>示例 JSON:
 * {"deviceCode":"D001","status":"RUNNING","temperature":98.5,"faultCode":null}
 */
public class DeviceStatusPayload {

    private String deviceCode;
    private String status;          // IDLE / RUNNING / FAULT / MAINTENANCE
    private BigDecimal temperature;
    private String faultCode;       // 可 null

    public DeviceStatusPayload() {
    }

    public DeviceStatusPayload(String deviceCode, String status, BigDecimal temperature, String faultCode) {
        this.deviceCode = deviceCode;
        this.status = status;
        this.temperature = temperature;
        this.faultCode = faultCode;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public String getFaultCode() {
        return faultCode;
    }

    public void setFaultCode(String faultCode) {
        this.faultCode = faultCode;
    }
}
