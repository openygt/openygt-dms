package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.AlertStatisticsDTO;
import cn.org.openygt.production.dto.TimeMonitorDashboardDTO;
import cn.org.openygt.production.dto.TimeRuleRequest;
import cn.org.openygt.production.entity.AlertLog;
import cn.org.openygt.production.entity.TimeMonitor;
import cn.org.openygt.production.entity.TimeRule;
import cn.org.openygt.production.mapper.AlertLogMapper;
import cn.org.openygt.production.mapper.TimeMonitorMapper;
import cn.org.openygt.production.mapper.TimeRuleMapper;
import cn.org.openygt.production.service.TimeMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeMonitorServiceImpl implements TimeMonitorService {

    private final TimeMonitorMapper timeMonitorMapper;
    private final TimeRuleMapper timeRuleMapper;
    private final AlertLogMapper alertLogMapper;

    @Override
    public TimeMonitorDashboardDTO getDashboard() {
        List<TimeMonitor> allMonitors = timeMonitorMapper.selectList(new LambdaQueryWrapper<TimeMonitor>()
                .orderByDesc(TimeMonitor::getUpdatedAt));
        List<TimeMonitor> monitors = allMonitors.stream()
                .filter(m -> m.getTaskId() != null)
                .filter(m -> m.getActualStart() != null)
                .filter(m -> m.getActualEnd() == null)
                .collect(Collectors.toMap(TimeMonitor::getTaskId, m -> m, this::pickDashboardMonitor))
                .values()
                .stream()
                .sorted(Comparator.comparing(TimeMonitor::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TimeMonitor::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        TimeMonitorDashboardDTO dto = new TimeMonitorDashboardDTO();
        dto.setTotalTasks(monitors.size());
        dto.setOnTimeTasks((int) monitors.stream().filter(this::isNormalMonitor).count());
        dto.setWarningTasks((int) monitors.stream().filter(this::isWarningMonitor).count());
        dto.setAlertTasks((int) monitors.stream().filter(this::isTimeoutMonitor).count());

        dto.setItems(monitors.stream().map(m -> {
            TimeMonitorDashboardDTO.TimeMonitorItemDTO item = new TimeMonitorDashboardDTO.TimeMonitorItemDTO();
            item.setMonitorId(m.getId());
            item.setTaskId(m.getTaskId());
            item.setPrescriptionId(m.getPrescriptionId());
            item.setStage(m.getStage());
            item.setPlannedStart(m.getPlannedStart());
            item.setPlannedEnd(m.getPlannedEnd());
            item.setActualStart(m.getActualStart());
            item.setActualEnd(m.getActualEnd());
            if (m.getActualEnd() != null) {
                item.setRemainingSeconds(0);
            } else if (m.getPlannedEnd() != null) {
                item.setRemainingSeconds((int) ChronoUnit.SECONDS.between(LocalDateTime.now(), m.getPlannedEnd()));
            } else {
                item.setRemainingSeconds(m.getRemainingSeconds());
            }
            item.setStatus(m.getStatus());
            item.setWarningCount(m.getWarningCount());
            return item;
        }).collect(Collectors.toList()));

        return dto;
    }

    @Override
    public List<TimeMonitor> getTaskMonitors(Long taskId) {
        LambdaQueryWrapper<TimeMonitor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeMonitor::getTaskId, taskId).orderByAsc(TimeMonitor::getPlannedStart);
        return timeMonitorMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public TimeRule saveTimeRule(TimeRuleRequest request) {
        TimeRule rule;
        if (request.getId() != null) {
            rule = timeRuleMapper.selectById(request.getId());
            if (rule == null) {
                throw new IllegalArgumentException("规则不存在");
            }
        } else {
            rule = new TimeRule();
            rule.setCreatedAt(LocalDateTime.now());
        }
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setPrescriptionType(request.getPrescriptionType());
        rule.setStage(request.getStage());
        rule.setStandardDuration(request.getStandardDuration());
        rule.setWarningThreshold(request.getWarningThreshold());
        rule.setAlertThreshold(request.getAlertThreshold());
        rule.setCriticalThreshold(request.getCriticalThreshold());
        rule.setIsDefault(request.getIsDefault());
        rule.setUpdatedAt(LocalDateTime.now());

        validateThresholds(rule);

        if (request.getId() != null) {
            timeRuleMapper.updateById(rule);
        } else {
            timeRuleMapper.insert(rule);
        }

        if (rule.getIsDefault() != null && rule.getIsDefault() == 1) {
            LambdaUpdateWrapper<TimeRule> clearDefaults = new LambdaUpdateWrapper<>();
            clearDefaults.eq(TimeRule::getPrescriptionType, rule.getPrescriptionType())
                    .eq(TimeRule::getStage, rule.getStage())
                    .eq(TimeRule::getIsDefault, 1)
                    .ne(TimeRule::getId, rule.getId())
                    .set(TimeRule::getIsDefault, 0)
                    .set(TimeRule::getUpdatedAt, LocalDateTime.now());
            timeRuleMapper.update(null, clearDefaults);
        }
        return rule;
    }

    @Override
    public List<TimeRule> listTimeRules() {
        LambdaQueryWrapper<TimeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(TimeRule::getId);
        return timeRuleMapper.selectList(wrapper);
    }

    @Override
    public IPage<AlertLog> listActiveAlerts(int page, int size) {
        LambdaQueryWrapper<AlertLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertLog::getIsResolved, 0).orderByDesc(AlertLog::getCreatedAt);
        return alertLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public AlertLog resolveAlert(Long alertId, Long resolvedBy) {
        AlertLog alert = alertLogMapper.selectById(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("预警记录不存在");
        }
        if (alert.getIsResolved() != null && alert.getIsResolved() == 1) {
            throw new IllegalStateException("预警已处理");
        }
        alert.setIsResolved(1);
        alert.setResolvedBy(resolvedBy);
        alert.setResolvedAt(LocalDateTime.now());
        alertLogMapper.updateById(alert);
        return alert;
    }

    @Override
    public AlertStatisticsDTO getAlertStatistics() {
        List<AlertLog> alerts = alertLogMapper.selectList(new LambdaQueryWrapper<>());

        AlertStatisticsDTO dto = new AlertStatisticsDTO();
        dto.setTotalAlerts(alerts.size());
        dto.setResolvedAlerts((int) alerts.stream().filter(a -> a.getIsResolved() != null && a.getIsResolved() == 1).count());
        dto.setUnresolvedAlerts((int) alerts.stream().filter(a -> a.getIsResolved() == null || a.getIsResolved() == 0).count());

        Map<String, Integer> levelDist = new HashMap<>();
        Map<String, Integer> typeDist = new HashMap<>();
        for (AlertLog alert : alerts) {
            String level = alert.getAlertLevel() != null ? String.valueOf(alert.getAlertLevel()) : "unknown";
            levelDist.merge(level, 1, Integer::sum);
            String type = alert.getAlertType() != null ? alert.getAlertType() : "unknown";
            typeDist.merge(type, 1, Integer::sum);
        }
        dto.setAlertLevelDistribution(levelDist);
        dto.setAlertTypeDistribution(typeDist);
        return dto;
    }

    private boolean isNormalMonitor(TimeMonitor monitor) {
        Integer status = monitor.getStatus();
        Integer alertLevel = monitor.getAlertLevel();
        return (status != null && (status == 1 || status == 2))
                && (alertLevel == null || alertLevel == 0);
    }

    private boolean isWarningMonitor(TimeMonitor monitor) {
        Integer status = monitor.getStatus();
        Integer alertLevel = monitor.getAlertLevel();
        return status != null && status != 3 && alertLevel != null && alertLevel == 1;
    }

    private boolean isTimeoutMonitor(TimeMonitor monitor) {
        Integer status = monitor.getStatus();
        Integer alertLevel = monitor.getAlertLevel();
        return (status != null && status == 3) || (alertLevel != null && alertLevel >= 2);
    }

    private void validateThresholds(TimeRule rule) {
        int standard = rule.getStandardDuration() != null ? rule.getStandardDuration() : 0;
        int warning = rule.getWarningThreshold() != null ? rule.getWarningThreshold() : 0;
        int alert = rule.getAlertThreshold() != null ? rule.getAlertThreshold() : 0;
        int critical = rule.getCriticalThreshold() != null ? rule.getCriticalThreshold() : 0;

        if (standard <= 0) {
            throw new IllegalArgumentException("标准时长必须大于0");
        }
        if (warning < 0 || alert < 0 || critical < 0) {
            throw new IllegalArgumentException("预警、超时、严重阈值不能小于0");
        }
        if (alert < warning) {
            throw new IllegalArgumentException("超时阈值不能小于预警阈值");
        }
        if (critical < alert) {
            throw new IllegalArgumentException("严重阈值不能小于超时阈值");
        }
    }

    private TimeMonitor pickDashboardMonitor(TimeMonitor left, TimeMonitor right) {
        Comparator<TimeMonitor> comparator = Comparator
                .comparing((TimeMonitor m) -> m.getActualEnd() == null ? 1 : 0)
                .thenComparing(m -> m.getUpdatedAt() != null ? m.getUpdatedAt() : m.getCreatedAt(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TimeMonitor::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        return comparator.compare(left, right) >= 0 ? left : right;
    }
}
