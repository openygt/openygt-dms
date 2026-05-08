package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.DeviceUtilization;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface DeviceUtilizationMapper extends BaseMapper<DeviceUtilization> {

    @Select("SELECT * FROM eq_device_utilization WHERE device_code = #{deviceCode} AND stat_date BETWEEN #{start} AND #{end} AND deleted = 0 ORDER BY stat_date")
    List<DeviceUtilization> findByDeviceAndDateRange(@Param("deviceCode") String deviceCode, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT * FROM eq_device_utilization WHERE stat_date = #{date} AND deleted = 0")
    List<DeviceUtilization> findByDate(@Param("date") LocalDate date);

    @Select("SELECT device_code, AVG(utilization_rate) as utilization_rate FROM eq_device_utilization WHERE stat_date BETWEEN #{start} AND #{end} AND deleted = 0 GROUP BY device_code")
    List<DeviceUtilization> aggregateByDevice(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT d.device_code, COUNT(*) as task_count " +
            "FROM eq_device d " +
            "LEFT JOIN prod_task t ON (t.decoct_device_id = d.id OR t.package_device_id = d.id) " +
            "WHERE d.deleted = 0 AND DATE(t.complete_time) = #{date} AND t.deleted = 0 " +
            "GROUP BY d.device_code")
    List<Map<String, Object>> selectTaskCountByDevice(@Param("date") String date);
}
