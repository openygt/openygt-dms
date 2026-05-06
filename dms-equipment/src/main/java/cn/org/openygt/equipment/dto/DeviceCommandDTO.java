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

    /** 指令等级: NORMAL/IMPORTANT/CRITICAL（后端推导，可选透传） */
    private String commandLevel;
    /** 风险等级: LOW/MEDIUM/HIGH（后端推导，可选透传） */
    private String riskLevel;
    /** 是否需要确认: 0否/1是（后端推导，可选透传） */
    private Integer requireConfirm;
}
