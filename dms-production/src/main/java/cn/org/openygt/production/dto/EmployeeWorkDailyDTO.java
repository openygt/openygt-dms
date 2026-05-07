package cn.org.openygt.production.dto;

import lombok.Data;
import java.util.List;

@Data
public class EmployeeWorkDailyDTO {
    private String operatorId;
    private String operatorName;
    private List<ActionSummary> actions;
    private Integer totalMinutes;

    @Data
    public static class ActionSummary {
        private String action;
        private String actionLabel;
        private Integer count;
        private Integer totalMinutes;
    }
}
