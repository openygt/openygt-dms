package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.DeviceCommand;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceCommandMapper extends BaseMapper<DeviceCommand> {

    @Select("SELECT * FROM device_command WHERE device_code = #{deviceCode} AND status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<DeviceCommand> findByDeviceCodeAndStatus(@Param("deviceCode") String deviceCode,
                                                   @Param("status") String status);

    @Select("SELECT * FROM device_command WHERE device_code = #{deviceCode} AND deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<DeviceCommand> findRecentByDeviceCode(@Param("deviceCode") String deviceCode,
                                                @Param("limit") int limit);

    @Select("SELECT * FROM device_command " +
            "WHERE device_code = #{deviceCode} AND command_type = #{commandType} " +
            "AND status IN ('PENDING', 'SENT') AND deleted = 0 " +
            "ORDER BY created_at DESC LIMIT 1")
    DeviceCommand findLatestDeliveringCommand(@Param("deviceCode") String deviceCode,
                                              @Param("commandType") String commandType);
}
