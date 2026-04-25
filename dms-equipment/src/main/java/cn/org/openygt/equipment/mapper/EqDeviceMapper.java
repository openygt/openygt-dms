package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqDevice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EqDeviceMapper extends BaseMapper<EqDevice> {

    /**
     * 通过设备编码查询。
     */
    @Select("SELECT * FROM eq_device WHERE device_code = #{deviceCode} AND deleted = 0")
    EqDevice findByDeviceCode(@Param("deviceCode") String deviceCode);

    /**
     * 悲观锁查询设备。
     * 必须在 @Transactional 内调用。
     * SQLite 忽略 FOR UPDATE（不报错），仅 MySQL 生产环境生效。
     */
    @Select("SELECT * FROM eq_device WHERE id = #{id} AND deleted = 0")
    EqDevice selectForUpdate(@Param("id") Long id);

    /**
     * 查询所有活跃设备（非 OFFLINE 且非 MAINTENANCE）。
     * 供心跳检测调度器使用。
     */
    @Select("SELECT * FROM eq_device WHERE deleted = 0 AND status != 'OFFLINE' AND status != 'MAINTENANCE'")
    List<EqDevice> findAllActive();
}
