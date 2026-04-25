package cn.org.openygt.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 设备生效温度阈值 DTO。
 *
 * <p>三层继承来源：
 * <ul>
 *   <li>SYSTEM — 系统默认（sys_config）</li>
 *   <li>DEVICE — 设备级（eq_device）</li>
 *   <li>SCHEME — 方案级（md_decoct_scheme，最高优先）</li>
 * </ul>
 */
public class TemperatureThresholdDTO implements Serializable {

    private String deviceCode;
    private BigDecimal highTemp;
    private BigDecimal lowTemp;
    private String source;    // SYSTEM / DEVICE / SCHEME

    public TemperatureThresholdDTO() {
    }

    public TemperatureThresholdDTO(String deviceCode, BigDecimal highTemp, BigDecimal lowTemp, String source) {
        this.deviceCode = deviceCode;
        this.highTemp = highTemp;
        this.lowTemp = lowTemp;
        this.source = source;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public BigDecimal getHighTemp() {
        return highTemp;
    }

    public void setHighTemp(BigDecimal highTemp) {
        this.highTemp = highTemp;
    }

    public BigDecimal getLowTemp() {
        return lowTemp;
    }

    public void setLowTemp(BigDecimal lowTemp) {
        this.lowTemp = lowTemp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
