package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqDeviceMqttConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EqDeviceMqttConfigMapper extends BaseMapper<EqDeviceMqttConfig> {

    @Select("SELECT * FROM iot_device_mqtt_config WHERE device_code = #{deviceCode} AND deleted = 0")
    EqDeviceMqttConfig findByDeviceCode(@Param("deviceCode") String deviceCode);
}
