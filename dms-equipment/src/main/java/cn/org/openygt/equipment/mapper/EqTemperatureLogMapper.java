package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.dto.TemperatureAggregationDTO;
import cn.org.openygt.equipment.entity.EqTemperatureLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EqTemperatureLogMapper extends BaseMapper<EqTemperatureLog> {

    /**
     * 按分钟聚合温度数据（MySQL语法）
     */
    @Select("SELECT " +
            "  DATE_FORMAT(recorded_at, '%Y-%m-%d %H:%i:00') as windowStart, " +
            "  ROUND(AVG(temperature), 2) as avgTemp, " +
            "  MAX(temperature) as maxTemp, " +
            "  MIN(temperature) as minTemp, " +
            "  COUNT(*) as sampleCount " +
            "FROM eq_temperature_log " +
            "WHERE device_id = #{deviceId} " +
            "  AND recorded_at >= #{start} AND recorded_at <= #{end} " +
            "GROUP BY DATE_FORMAT(recorded_at, '%Y-%m-%d %H:%i') " +
            "ORDER BY windowStart")
    List<TemperatureAggregationDTO> aggregateByMinute(@Param("deviceId") Long deviceId,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

    /**
     * 按5分钟聚合温度数据（MySQL语法）
     */
    @Select("SELECT " +
            "  DATE_FORMAT(DATE_SUB(recorded_at, INTERVAL (MINUTE(recorded_at) % 5) MINUTE), '%Y-%m-%d %H:%i:00') as windowStart, " +
            "  ROUND(AVG(temperature), 2) as avgTemp, " +
            "  MAX(temperature) as maxTemp, " +
            "  MIN(temperature) as minTemp, " +
            "  COUNT(*) as sampleCount " +
            "FROM eq_temperature_log " +
            "WHERE device_id = #{deviceId} " +
            "  AND recorded_at >= #{start} AND recorded_at <= #{end} " +
            "GROUP BY DATE_FORMAT(DATE_SUB(recorded_at, INTERVAL (MINUTE(recorded_at) % 5) MINUTE), '%Y-%m-%d %H:%i') " +
            "ORDER BY windowStart")
    List<TemperatureAggregationDTO> aggregateBy5Minute(@Param("deviceId") Long deviceId,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    /**
     * 按小时聚合温度数据（MySQL语法）
     */
    @Select("SELECT " +
            "  DATE_FORMAT(recorded_at, '%Y-%m-%d %H:00:00') as windowStart, " +
            "  ROUND(AVG(temperature), 2) as avgTemp, " +
            "  MAX(temperature) as maxTemp, " +
            "  MIN(temperature) as minTemp, " +
            "  COUNT(*) as sampleCount " +
            "FROM eq_temperature_log " +
            "WHERE device_id = #{deviceId} " +
            "  AND recorded_at >= #{start} AND recorded_at <= #{end} " +
            "GROUP BY DATE_FORMAT(recorded_at, '%Y-%m-%d %H') " +
            "ORDER BY windowStart")
    List<TemperatureAggregationDTO> aggregateByHour(@Param("deviceId") Long deviceId,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);
}
