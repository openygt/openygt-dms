package cn.org.openygt.iot.adapter;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 下发给设备的指令数据对象
 */
@Data
public class DeviceCommandDTO {

    /** 指令类型，如 START_SOAK, START_DECOCT, PAUSE, STOP 等 */
    private String commandType;

    /** 任务编号（可选） */
    private String taskId;

    /** 扩展参数 */
    private Map<String, Object> params = new HashMap<>();
}
