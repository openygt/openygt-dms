package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.AlarmConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlarmConfigMapper extends BaseMapper<AlarmConfig> {

    @Select("SELECT * FROM eq_alarm_config WHERE alarm_type = #{alarmType} AND enabled = 1 AND deleted = 0")
    List<AlarmConfig> findEnabledByType(@Param("alarmType") String alarmType);

    @Select("SELECT * FROM eq_alarm_config WHERE enabled = 1 AND deleted = 0")
    List<AlarmConfig> findAllEnabled();
}
