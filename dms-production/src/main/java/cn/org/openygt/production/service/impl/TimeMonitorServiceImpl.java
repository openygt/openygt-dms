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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        List<TimeMonitor> monitors = timeMonitorMapper.selectList(new LambdaQueryWrapper<TimeMonitor>()
                .orderByDesc(TimeMonitor::getUpdatedAt));

        TimeMonitorDashboardDTO dto = new TimeMonitorDashboardDTO();
        dto.setTotalTasks(monitors.size());
        dto.setOnTimeTasks((int) monitors.stream().filter(m -> m.getStatus() != null && m.getStatus() == 0).count());
        dto.setWarningTasks((int) monitors.stream().filter(m -> m.getStatus() != null && m.getStatus() == 1).count());
        dto.setAlertTasks((int) monitors.stream().filter(m -> m.getStatus() != null && m.getStatus() >= 2).count());

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
            // 实时计算剩余秒数
            if (m.getActualEnd() != null) {
                item.setRemainingSeconds(0);
            } else if (m.getPlannedEnd() != null) {
                long secs = ChronoUnit.SECONDS.between(LocalDateTime.now(), m.getPlannedEnd());
                item.setRemainingSeconds((int) Math.max(0, secs));
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

        if (request.getId() != null) {
            timeRuleMapper.updateById(rule);
        } else {
            timeRuleMapper.insert(rule);
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
}
