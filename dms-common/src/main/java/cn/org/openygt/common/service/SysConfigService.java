package cn.org.openygt.common.service;

/**
 * 系统配置查询 SPI。
 * 定义在 dms-common，由 dms-system 实现。
 * 供其他模块读取系统配置项（如设备心跳超时阈值）。
 */
public interface SysConfigService {

    String getStringValue(String key, String defaultValue);

    Integer getIntValue(String key, Integer defaultValue);
}
