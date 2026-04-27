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
            "SUM(CASE WHEN status = '已完成' OR status = '已部分完成' THEN 1 ELSE 0 END) as completed, " +
            "SUM(CASE WHEN status IN ('泡药中','煎药中','出液中','包装中') THEN 1 ELSE 0 END) as inProgress, " +
            "SUM(CASE WHEN status = '待泡药' THEN 1 ELSE 0 END) as pending " +
            "FROM prod_task WHERE deleted = 0 AND DATE(created_at) = CURDATE()")
    Map<String, Object> selectTodayTaskStats();

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

    @Select("SELECT device_type, COUNT(*) as count FROM eq_device WHERE deleted = 0 GROUP BY device_type")
    java.util.List<Map<String, Object>> selectDeviceTypeDistribution();
}
