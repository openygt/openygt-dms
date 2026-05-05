package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqDeviceStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EqDeviceStatusMapper extends BaseMapper<EqDeviceStatus> {

    @Select("SELECT * FROM eq_device_status WHERE device_code = #{deviceCode} ORDER BY snapshot_time DESC LIMIT 1")
    EqDeviceStatus findLatestByDeviceCode(@Param("deviceCode") String deviceCode);

    @Select("SELECT * FROM eq_device_status WHERE device_code = #{deviceCode} AND snapshot_time >= #{startTime} AND snapshot_time <= #{endTime} ORDER BY snapshot_time ASC")
    List<EqDeviceStatus> findByDeviceCodeAndTimeRange(@Param("deviceCode") String deviceCode,
                                                       @Param("startTime") String startTime,
                                                       @Param("endTime") String endTime);

    @Select("SELECT * FROM eq_device_status WHERE snapshot_time >= #{startTime} AND snapshot_time <= #{endTime} ORDER BY device_code, snapshot_time ASC")
    List<EqDeviceStatus> findAllByTimeRange(@Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("SELECT * FROM eq_device_status WHERE device_code = #{deviceCode} AND snapshot_time < #{beforeTime} ORDER BY snapshot_time DESC LIMIT 1")
    EqDeviceStatus findLatestBeforeTime(@Param("deviceCode") String deviceCode, @Param("beforeTime") String beforeTime);
}
