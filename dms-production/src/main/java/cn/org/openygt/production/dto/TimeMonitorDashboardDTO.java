package cn.org.openygt.production.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TimeMonitorDashboardDTO {
    private Integer totalTasks;
    private Integer onTimeTasks;
    private Integer warningTasks;
    private Integer alertTasks;
    private List<TimeMonitorItemDTO> items;

    @Data
    public static class TimeMonitorItemDTO {
        private Long monitorId;
        private Long taskId;
        private Long prescriptionId;
        private String stage;
        private LocalDateTime plannedStart;
        private LocalDateTime plannedEnd;
        private LocalDateTime actualStart;
        private LocalDateTime actualEnd;
        private Integer remainingSeconds;
        private Integer status;
        private Integer warningCount;
    }
}
