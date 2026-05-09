package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.WorkloadStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface WorkloadStatMapper extends BaseMapper<WorkloadStat> {

    @Select("SELECT * FROM ops_workload_stat WHERE stat_date = #{date} AND deleted = 0")
    List<WorkloadStat> findByDate(@Param("date") LocalDate date);

    @Select("SELECT work_type, SUM(prescription_count) as count FROM ops_workload_stat WHERE stat_date BETWEEN #{start} AND #{end} AND deleted = 0 GROUP BY work_type")
    List<Map<String, Object>> aggregateByType(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT * FROM ops_workload_stat WHERE operator_id = #{operatorId} AND stat_date = #{date} AND deleted = 0")
    List<WorkloadStat> findByOperatorAndDate(@Param("operatorId") Long operatorId, @Param("date") LocalDate date);
}
