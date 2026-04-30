package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.DeviceUtilization;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DeviceUtilizationMapper extends BaseMapper<DeviceUtilization> {

    @Select("SELECT * FROM device_utilization WHERE device_code = #{deviceCode} AND stat_date BETWEEN #{start} AND #{end} AND deleted = 0 ORDER BY stat_date")
    List<DeviceUtilization> findByDeviceAndDateRange(@Param("deviceCode") String deviceCode, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT * FROM device_utilization WHERE stat_date = #{date} AND deleted = 0")
    List<DeviceUtilization> findByDate(@Param("date") LocalDate date);

    @Select("SELECT device_code, AVG(utilization_rate) as avg_utilization FROM device_utilization WHERE stat_date BETWEEN #{start} AND #{end} AND deleted = 0 GROUP BY device_code")
    List<DeviceUtilization> aggregateByDevice(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
