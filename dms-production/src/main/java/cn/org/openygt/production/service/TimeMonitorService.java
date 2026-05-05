package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.AlertStatisticsDTO;
import cn.org.openygt.production.dto.TimeMonitorDashboardDTO;
import cn.org.openygt.production.dto.TimeRuleRequest;
import cn.org.openygt.production.entity.AlertLog;
import cn.org.openygt.production.entity.TimeMonitor;
import cn.org.openygt.production.entity.TimeRule;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface TimeMonitorService {

    TimeMonitorDashboardDTO getDashboard();

    com.baomidou.mybatisplus.core.metadata.IPage<TimeMonitorDashboardDTO.TimeMonitorItemDTO> listMonitorsByCategory(String category, int page, int size);

    List<TimeMonitor> getTaskMonitors(Long taskId);

    TimeRule saveTimeRule(TimeRuleRequest request);

    List<TimeRule> listTimeRules();

    IPage<AlertLog> listActiveAlerts(int page, int size);

    AlertLog resolveAlert(Long alertId, Long resolvedBy);

    AlertStatisticsDTO getAlertStatistics();
}
