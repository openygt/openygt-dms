package cn.org.openygt.equipment.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 设备控制指令 DTO。
 */
@Data
public class DeviceCommandDTO implements Serializable {

    private String action;
    private Map<String, Object> params;
}
