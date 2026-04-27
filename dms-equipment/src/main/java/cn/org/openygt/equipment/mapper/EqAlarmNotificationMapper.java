package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqAlarmNotification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EqAlarmNotificationMapper extends BaseMapper<EqAlarmNotification> {

    @Select("SELECT * FROM eq_alarm_notification WHERE alarm_id = #{alarmId} AND deleted = 0 ORDER BY created_at DESC")
    List<EqAlarmNotification> findByAlarmId(@Param("alarmId") Long alarmId);
}
