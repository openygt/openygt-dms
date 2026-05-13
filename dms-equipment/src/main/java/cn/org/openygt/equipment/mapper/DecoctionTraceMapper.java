package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.DecoctionTrace;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DecoctionTraceMapper extends BaseMapper<DecoctionTrace> {

    @Select("SELECT * FROM trc_prescription_trace WHERE prescription_no = #{prescriptionNo} AND deleted = 0")
    DecoctionTrace findByPrescriptionNo(@Param("prescriptionNo") String prescriptionNo);

    @Select("SELECT * FROM trc_prescription_trace WHERE decoct_device_code = #{deviceCode} AND deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<DecoctionTrace> findByDeviceCode(@Param("deviceCode") String deviceCode, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM trc_prescription_trace WHERE status = #{status} AND deleted = 0 AND DATE(created_at) = CURDATE()")
    Long countByStatusToday(@Param("status") String status);

    @Select("SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, created_at, complete_time)), 0) " +
            "FROM trc_prescription_trace " +
            "WHERE status = 'COMPLETED' AND deleted = 0 AND complete_time IS NOT NULL")
    Double selectAvgProcessTimeMinutes();
}
