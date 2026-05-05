package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.entity.EmergencyPrescription;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskAssignment;
import cn.org.openygt.production.entity.TimeRule;
import cn.org.openygt.production.mapper.EmergencyPrescriptionMapper;
import cn.org.openygt.production.mapper.TimeRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TimeMonitorRuleResolver {

    private static final Set<String> SOAK_STATUSES = new HashSet<>(Arrays.asList("待泡药", "泡药中"));
    private static final Set<String> DECOCT_STATUSES = new HashSet<>(Arrays.asList("待煎药", "煎药中", "待出液", "出液中"));
    private static final Set<String> WRAP_STATUSES = new HashSet<>(Arrays.asList("待包装", "包装中"));

    private final TimeRuleMapper timeRuleMapper;
    private final EmergencyPrescriptionMapper emergencyPrescriptionMapper;

    public StageContext resolveStageContext(Task task, TaskAssignment assignment, LocalDateTime now) {
        String stage = resolveCurrentStage(task);
        if (stage == null) {
            return null;
        }

        TimeRule rule = resolveBestRule(task, stage);
        LocalDateTime plannedStart = resolvePlannedStart(task, assignment, stage, now);
        if (plannedStart == null) {
            plannedStart = now;
        }

        LocalDateTime actualStart = resolveActualStart(task, stage);
        LocalDateTime plannedEnd = resolvePlannedEnd(rule, plannedStart, assignment, stage);
        int status = actualStart != null ? 2 : 1;
        return new StageContext(stage, plannedStart, plannedEnd, actualStart, status, rule);
    }

    public String resolveCurrentStage(Task task) {
        if (task == null) {
            return null;
        }

        String currentStep = normalizeStage(task.getCurrentStep());
        String status = task.getStatus();
        if (status == null || status.trim().isEmpty()) {
            return currentStep;
        }

        if (SOAK_STATUSES.contains(status)) {
            return "SOAK";
        }
        if (DECOCT_STATUSES.contains(status)) {
            if ("FIRST_DECOCTION".equals(currentStep) || "SECOND_DECOCTION".equals(currentStep) || "DECOCT".equals(currentStep)) {
                return currentStep;
            }
            return "DECOCT";
        }
        if (WRAP_STATUSES.contains(status)) {
            return "WRAP";
        }
        return null;
    }

    public TimeRule resolveBestRule(Task task, String stage) {
        String prescriptionType = resolvePrescriptionType(task);

        TimeRule exactRule = timeRuleMapper.selectOne(new LambdaQueryWrapper<TimeRule>()
                .eq(TimeRule::getPrescriptionType, prescriptionType)
                .eq(TimeRule::getStage, stage)
                .orderByDesc(TimeRule::getIsDefault)
                .orderByAsc(TimeRule::getId)
                .last("LIMIT 1"));
        if (exactRule != null) {
            return exactRule;
        }

        if (!"NORMAL".equals(prescriptionType)) {
            return timeRuleMapper.selectOne(new LambdaQueryWrapper<TimeRule>()
                    .eq(TimeRule::getPrescriptionType, "NORMAL")
                    .eq(TimeRule::getStage, stage)
                    .eq(TimeRule::getIsDefault, 1)
                    .orderByAsc(TimeRule::getId)
                    .last("LIMIT 1"));
        }
        return null;
    }

    public String resolvePrescriptionType(Task task) {
        if (task == null) {
            return "NORMAL";
        }

        if (task.getPrescriptionId() != null) {
            EmergencyPrescription emergency = emergencyPrescriptionMapper.selectOne(new LambdaQueryWrapper<EmergencyPrescription>()
                    .eq(EmergencyPrescription::getPrescriptionId, task.getPrescriptionId())
                    .orderByDesc(EmergencyPrescription::getUpdatedAt)
                    .orderByDesc(EmergencyPrescription::getId)
                    .last("LIMIT 1"));
            if (emergency != null && emergency.getEmergencyLevel() != null) {
                return emergency.getEmergencyLevel() >= 2 ? "CRITICAL_EMERGENCY" : "EMERGENCY";
            }
        }

        if (task.getIsEmergency() != null && task.getIsEmergency() == 1) {
            return task.getPriority() != null && task.getPriority() <= 2 ? "CRITICAL_EMERGENCY" : "EMERGENCY";
        }
        return "NORMAL";
    }

    public LocalDateTime resolveActualEnd(Task task, String stage) {
        if (task == null || stage == null) {
            return null;
        }
        switch (stage) {
            case "SOAK":
                return task.getSoakEndTime();
            case "FIRST_DECOCTION":
            case "SECOND_DECOCTION":
            case "DECOCT":
                return task.getPourEndTime() != null ? task.getPourEndTime() : task.getDecoctEndTime();
            case "WRAP":
                return task.getWrapEndTime();
            default:
                return null;
        }
    }

    private LocalDateTime resolvePlannedStart(Task task, TaskAssignment assignment, String stage, LocalDateTime now) {
        if ("SOAK".equals(stage)) {
            if (task.getSoakStartTime() != null) {
                return task.getSoakStartTime();
            }
            if (assignment != null && assignment.getScheduledStartTime() != null) {
                return assignment.getScheduledStartTime();
            }
        } else if ("FIRST_DECOCTION".equals(stage) || "SECOND_DECOCTION".equals(stage) || "DECOCT".equals(stage)) {
            if (task.getDecoctStartTime() != null) {
                return task.getDecoctStartTime();
            }
            if (task.getSoakEndTime() != null) {
                return task.getSoakEndTime();
            }
            if (assignment != null && assignment.getScheduledStartTime() != null) {
                return assignment.getScheduledStartTime();
            }
        } else if ("WRAP".equals(stage)) {
            if (task.getWrapStartTime() != null) {
                return task.getWrapStartTime();
            }
            if (task.getPourEndTime() != null) {
                return task.getPourEndTime();
            }
            if (task.getDecoctEndTime() != null) {
                return task.getDecoctEndTime();
            }
            if (assignment != null && assignment.getScheduledEndTime() != null) {
                return assignment.getScheduledEndTime();
            }
        }

        if (assignment != null && assignment.getCreatedAt() != null) {
            return assignment.getCreatedAt();
        }
        return now;
    }

    public LocalDateTime resolveActualStart(Task task, String stage) {
        if (task == null || stage == null) {
            return null;
        }
        switch (stage) {
            case "SOAK":
                return task.getSoakStartTime();
            case "FIRST_DECOCTION":
            case "SECOND_DECOCTION":
            case "DECOCT":
                return task.getDecoctStartTime();
            case "WRAP":
                return task.getWrapStartTime();
            default:
                return null;
        }
    }

    private LocalDateTime resolvePlannedEnd(TimeRule rule, LocalDateTime plannedStart, TaskAssignment assignment, String stage) {
        if (plannedStart != null && rule != null && rule.getStandardDuration() != null && rule.getStandardDuration() > 0) {
            return plannedStart.plusMinutes(rule.getStandardDuration());
        }
        if (assignment != null && assignment.getScheduledEndTime() != null) {
            return assignment.getScheduledEndTime();
        }
        if (plannedStart == null) {
            return null;
        }
        if ("SOAK".equals(stage)) {
            return plannedStart.plusMinutes(30);
        }
        if ("WRAP".equals(stage)) {
            return plannedStart.plusMinutes(20);
        }
        return plannedStart.plusHours(8);
    }

    private String normalizeStage(String stage) {
        if (stage == null || stage.trim().isEmpty()) {
            return null;
        }
        if ("PACKING".equals(stage)) {
            return "WRAP";
        }
        return stage;
    }

    @Getter
    public static class StageContext {
        private final String stage;
        private final LocalDateTime plannedStart;
        private final LocalDateTime plannedEnd;
        private final LocalDateTime actualStart;
        private final int status;
        private final TimeRule rule;

        public StageContext(String stage,
                            LocalDateTime plannedStart,
                            LocalDateTime plannedEnd,
                            LocalDateTime actualStart,
                            int status,
                            TimeRule rule) {
            this.stage = stage;
            this.plannedStart = plannedStart;
            this.plannedEnd = plannedEnd;
            this.actualStart = actualStart;
            this.status = status;
            this.rule = rule;
        }
    }
}
