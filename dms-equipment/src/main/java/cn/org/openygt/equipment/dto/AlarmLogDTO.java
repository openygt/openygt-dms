package cn.org.openygt.equipment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警日志DTO，用于前端展示。
 */
@Data
public class AlarmLogDTO {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private String alarmType;
    private String alarmLevel;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
