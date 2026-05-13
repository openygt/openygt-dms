package cn.org.openygt.analytics.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 监控大屏统计查询（跨表原生SQL）。
 */
@Mapper
public interface DashboardStatMapper {

    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'COMPLETED' OR status = 'PARTIAL_COMPLETED' THEN 1 ELSE 0 END) as ended, " +
            "SUM(CASE WHEN status NOT IN ('COMPLETED', 'PARTIAL_COMPLETED', 'CANCELLED', 'SCRAPPED') THEN 1 ELSE 0 END) as inProgress, " +
            "SUM(CASE WHEN is_exception = 1 AND status NOT IN ('COMPLETED', 'PARTIAL_COMPLETED', 'CANCELLED', 'SCRAPPED') THEN 1 ELSE 0 END) as alertingNow, " +
            "SUM(CASE WHEN is_exception = 1 THEN 1 ELSE 0 END) as everAlerted " +
            "FROM prod_task WHERE deleted = 0 AND DATE(created_at) = CURDATE()")
    Map<String, Object> selectTodayTaskStats();

    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'COMPLETED' OR status = 'PARTIAL_COMPLETED' THEN 1 ELSE 0 END) as ended, " +
            "SUM(CASE WHEN status IN ('SOAKING','DECOCTING','POURING','WRAPPING') THEN 1 ELSE 0 END) as inProgress, " +
            "SUM(CASE WHEN status = 'WAIT_SOAK' THEN 1 ELSE 0 END) as pending " +
            "FROM prod_task WHERE deleted = 0 AND DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)")
    Map<String, Object> selectYesterdayTaskStats();

    @Select("SELECT COUNT(*) as online FROM eq_device WHERE deleted = 0 AND status = 'ONLINE'")
    Long selectOnlineDeviceCount();

    @Select("SELECT COUNT(*) as offline FROM eq_device WHERE deleted = 0 AND status = 'OFFLINE'")
    Long selectOfflineDeviceCount();

    @Select("SELECT COUNT(*) as alarms FROM eq_device_alarm WHERE is_resolved = 0 AND deleted = 0")
    Long selectActiveAlarmCount();

    @Select("SELECT COUNT(*) as total FROM qt_inspection WHERE DATE(created_at) = CURDATE()")
    Long selectTodayInspectionCount();

    @Select("SELECT status, COUNT(*) as count FROM prod_task WHERE deleted = 0 GROUP BY status")
    java.util.List<Map<String, Object>> selectTaskStatusDistribution();

    @Select("SELECT device_type, " +
            "COUNT(*) as count, " +
            "SUM(CASE WHEN status = 'ONLINE' THEN 1 ELSE 0 END) as onlineCount, " +
            "SUM(CASE WHEN status = 'ONLINE' AND id IN (" +
            "  SELECT device_id FROM eq_device_alarm WHERE is_resolved = 0 AND deleted = 0" +
            ") THEN 1 ELSE 0 END) as onlineWithAlarmCount, " +
            "SUM(CASE WHEN status = 'OFFLINE' OR status = 'FAULT' THEN 1 ELSE 0 END) as offlineCount " +
            "FROM eq_device WHERE deleted = 0 GROUP BY device_type")
    java.util.List<Map<String, Object>> selectDeviceTypeDistribution();
}
