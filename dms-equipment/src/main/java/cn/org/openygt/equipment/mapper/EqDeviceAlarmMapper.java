package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface EqDeviceAlarmMapper extends BaseMapper<EqDeviceAlarm> {

    @Select("SELECT * FROM eq_device_alarm WHERE device_id = #{deviceId} AND alarm_type = #{type} AND is_resolved = 0 AND deleted = 0 ORDER BY created_at DESC LIMIT 1")
    EqDeviceAlarm findLatestActiveAlarm(@Param("deviceId") Long deviceId, @Param("type") String alarmType);

    @Select("SELECT * FROM eq_device_alarm WHERE device_id = #{deviceId} AND alarm_type = #{type} AND is_resolved = 0 AND deleted = 0")
    List<EqDeviceAlarm> findActiveByDeviceIdAndType(@Param("deviceId") Long deviceId, @Param("type") String alarmType);

    @Select("SELECT d.device_code AS deviceCode, a.alarm_type AS alarmType, COUNT(*) AS count " +
            "FROM eq_device_alarm a LEFT JOIN eq_device d ON a.device_id = d.id " +
            "WHERE a.deleted = 0 " +
            "GROUP BY a.device_id, a.alarm_type " +
            "ORDER BY count DESC")
    List<Map<String, Object>> selectAlarmStatsGrouped();
}
