package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqDevicePairing;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EqDevicePairingMapper extends BaseMapper<EqDevicePairing> {

    /**
     * 根据煎药机设备ID查找对应的配对记录
     */
    @Select("SELECT * FROM eq_device_pairing WHERE FIND_IN_SET(#{decoctDeviceId}, decocter_ids) AND status = 'ACTIVE' AND deleted = 0 LIMIT 1")
    EqDevicePairing findByDecocterId(@Param("decoctDeviceId") Long decoctDeviceId);
}
