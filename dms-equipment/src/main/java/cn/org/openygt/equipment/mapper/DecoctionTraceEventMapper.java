package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.DecoctionTraceEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DecoctionTraceEventMapper extends BaseMapper<DecoctionTraceEvent> {

    @Select("SELECT * FROM trc_trace_event WHERE prescription_no = #{prescriptionNo} AND deleted = 0 ORDER BY event_time")
    List<DecoctionTraceEvent> findByPrescriptionNo(@Param("prescriptionNo") String prescriptionNo);

    @Select("SELECT * FROM trc_trace_event WHERE trace_id = #{traceId} AND deleted = 0 ORDER BY event_time")
    List<DecoctionTraceEvent> findByTraceId(@Param("traceId") Long traceId);
}
