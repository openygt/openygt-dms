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
import java.util.Collections;
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
        List<TimeMonitor> monitors = getEffectiveMonitors();
        Map<Long, List<AlertLog>> alertMap = getAlertMap(monitors);

        // 分类统计
        int normalCount = 0;
        int warningCount = 0;
        int timeoutCount = 0;
        int resolvedCount = 0;

        for (TimeMonitor monitor : monitors) {
            Category category = classifyMonitor(monitor, alertMap.getOrDefault(monitor.getTaskId(), Collections.emptyList()));
            switch (category) {
                case NORMAL: normalCount++; break;
                case WARNING: warningCount++; break;
                case TIMEOUT: timeoutCount++; break;
                case RESOLVED: resolvedCount++; break;
            }
        }

        TimeMonitorDashboardDTO dto = new TimeMonitorDashboardDTO();
        dto.setTotalTasks(monitors.size());
        dto.setOnTimeTasks(normalCount);
        dto.setWarningTasks(warningCount);
        dto.setAlertTasks(timeoutCount);
        dto.setResolvedTasks(resolvedCount);

        return dto;
    }

    @Override
    public IPage<TimeMonitorDashboardDTO.TimeMonitorItemDTO> listMonitorsByCategory(String category, int page, int size) {
        // 参数归一化与校验
        if (page < 1) page = 1;
        if (size < 1) size = 1;
        if (size > 500) size = 500;

        List<TimeMonitor> monitors = getEffectiveMonitors();
        Map<Long, List<AlertLog>> alertMap = getAlertMap(monitors);

        // 按 category 筛选
        List<TimeMonitor> filtered;
        if (category == null || category.trim().isEmpty() || "all".equalsIgnoreCase(category)) {
            filtered = monitors;
        } else {
            final Category target = Category.valueOf(category.toUpperCase());
            filtered = monitors.stream()
                    .filter(m -> classifyMonitor(m, alertMap.getOrDefault(m.getTaskId(), Collections.emptyList())) == target)
                    .collect(Collectors.toList());
        }

        // 手动分页
        int total = filtered.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<TimeMonitor> pageRecords = fromIndex < total ? filtered.subList(fromIndex, toIndex) : Collections.emptyList();

        List<TimeMonitorDashboardDTO.TimeMonitorItemDTO> items = pageRecords.stream()
                .map(this::buildItemDTO)
                .collect(Collectors.toList());

        IPage<TimeMonitorDashboardDTO.TimeMonitorItemDTO> result = new Page<>(page, size);
        result.setTotal(total);
        result.setRecords(items);
        return result;
    }

    /**
     * 收敛“当前有效监控任务”集合
     */
    private List<TimeMonitor> getEffectiveMonitors() {
        List<TimeMonitor> monitors = timeMonitorMapper.selectList(new LambdaQueryWrapper<TimeMonitor>()
                .isNotNull(TimeMonitor::getTaskId)
                .isNotNull(TimeMonitor::getActualStart)
                .isNull(TimeMonitor::getActualEnd)
                .orderByDesc(TimeMonitor::getUpdatedAt));
        return monitors.stream()
                .collect(Collectors.toMap(TimeMonitor::getTaskId, m -> m, this::pickDashboardMonitor))
                .values()
                .stream()
                .sorted(Comparator.comparing(TimeMonitor::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TimeMonitor::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 批量查询 AlertLog 并按 taskId 分组
     */
    private Map<Long, List<AlertLog>> getAlertMap(List<TimeMonitor> monitors) {
        List<Long> taskIds = monitors.stream().map(TimeMonitor::getTaskId).distinct().collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AlertLog> allAlerts = alertLogMapper.selectList(
                new LambdaQueryWrapper<AlertLog>()
                        .in(AlertLog::getTaskId, taskIds));
        return allAlerts.stream().collect(Collectors.groupingBy(AlertLog::getTaskId));
    }

    private TimeMonitorDashboardDTO.TimeMonitorItemDTO buildItemDTO(TimeMonitor m) {
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
            int secs = (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), m.getPlannedEnd());
            item.setRemainingSeconds(Math.max(0, secs));
        } else {
            item.setRemainingSeconds(m.getRemainingSeconds());
        }
        item.setStatus(m.getStatus());
        item.setAlertLevel(m.getAlertLevel());
        item.setWarningCount(m.getWarningCount());
        return item;
    }

    /**
     * 任务分类枚举
     */
    private enum Category {
        TIMEOUT, WARNING, RESOLVED, NORMAL
    }

    /**
     * 对单个当前有效监控任务进行分类
     */
    private Category classifyMonitor(TimeMonitor monitor, List<AlertLog> alerts) {
        boolean hasUnresolved = alerts.stream().anyMatch(a -> a.getIsResolved() == null || a.getIsResolved() == 0);
        boolean hasResolved = alerts.stream().anyMatch(a -> a.getIsResolved() != null && a.getIsResolved() == 1);
        Integer alertLevel = monitor.getAlertLevel();

        // 1. 先判断是否超时
        if (alertLevel != null && alertLevel >= 2) {
            return Category.TIMEOUT;
        }
        if (monitor.getStatus() != null && monitor.getStatus() == 3) {
            return Category.TIMEOUT;
        }

        // 2. 再判断是否预警中
        if (alertLevel != null && alertLevel == 1 && hasUnresolved) {
            return Category.WARNING;
        }

        // 3. 再判断是否存在已处理预警且当前无未处理告警
        if (!hasUnresolved && hasResolved) {
            return Category.RESOLVED;
        }

        // 4. 剩余算正常
        return Category.NORMAL;
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
        // 校验 ruleCode 唯一性
        Long existCount = timeRuleMapper.selectCount(
                new LambdaQueryWrapper<TimeRule>()
                        .eq(TimeRule::getRuleCode, request.getRuleCode())
                        .ne(request.getId() != null, TimeRule::getId, request.getId()));
        if (existCount != null && existCount > 0) {
            throw new IllegalArgumentException("规则编码已存在: " + request.getRuleCode());
        }

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

        // 若该监控实例下所有预警均已处理，则恢复监控状态
        boolean hasUnresolved = alertLogMapper.selectCount(
                new LambdaQueryWrapper<AlertLog>()
                        .eq(AlertLog::getMonitorId, alert.getMonitorId())
                        .eq(AlertLog::getIsResolved, 0)
                        .last("LIMIT 1")) > 0;
        if (!hasUnresolved) {
            TimeMonitor monitor = timeMonitorMapper.selectById(alert.getMonitorId());
            if (monitor != null) {
                if (monitor.getActualEnd() != null) {
                    monitor.setStatus(4); // 已完成归档
                } else if (monitor.getAlertLevel() != null && monitor.getAlertLevel() == 1) {
                    // 仅预警级别，恢复为正常/运行中状态
                    monitor.setStatus(monitor.getActualStart() != null ? 2 : 1);
                    monitor.setAlertLevel(0);
                }
                monitor.setUpdatedAt(LocalDateTime.now());
                timeMonitorMapper.updateById(monitor);
            }
        }
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
