package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqDeviceOperator;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface EqDeviceOperatorMapper extends BaseMapper<EqDeviceOperator> {

    @Select("SELECT * FROM eq_device_operator WHERE device_code = #{deviceCode} AND is_current = 1 AND deleted = 0 ORDER BY shift_start_time DESC LIMIT 1")
    EqDeviceOperator findCurrentByDeviceCode(@Param("deviceCode") String deviceCode);

    @Select("SELECT * FROM eq_device_operator WHERE device_code = #{deviceCode} AND deleted = 0 ORDER BY shift_start_time DESC")
    List<EqDeviceOperator> findByDeviceCode(@Param("deviceCode") String deviceCode);

    @Update("UPDATE eq_device_operator SET is_current = 0 WHERE device_code = #{deviceCode} AND is_current = 1 AND deleted = 0")
    int clearCurrentByDeviceCode(@Param("deviceCode") String deviceCode);
}
