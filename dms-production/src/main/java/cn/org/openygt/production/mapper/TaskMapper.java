package cn.org.openygt.production.mapper;

import cn.org.openygt.production.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 悲观锁查询 —— MySQL 迁移后生效（SQLite 不支持 FOR UPDATE）。
     */
    @Select("SELECT * FROM prod_task WHERE id = #{id} AND deleted = 0")
    Task selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM prod_task WHERE barcode = #{barcode} AND deleted = 0 LIMIT 1")
    Task selectByBarcode(@Param("barcode") String barcode);

    /**
     * 查询指定日期范围内的任务列表（用于产能统计）。
     */
    @Select("SELECT * FROM prod_task WHERE deleted = 0 AND DATE(created_at) BETWEEN #{startDate} AND #{endDate}")
    List<Task> selectByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 日产能聚合统计（按任务完成日期统计）。
     */
    @Select("SELECT DATE(t.complete_time) as statDate, COUNT(*) as totalTasks, " +
            "SUM(CASE WHEN t.status IN ('COMPLETED', '已完成', '已部分完成') THEN 1 ELSE 0 END) as completedTasks, " +
            "SUM(CASE WHEN t.status IN ('COMPLETED', '已完成', '已部分完成') " +
            "     THEN COALESCE(p.repetition * p.bags_per_repetition, 1) ELSE 0 END) as doseCount, " +
            "AVG(CASE WHEN t.status IN ('COMPLETED', '已完成', '已部分完成') AND t.complete_time IS NOT NULL " +
            "     THEN TIMESTAMPDIFF(MINUTE, t.created_at, t.complete_time) ELSE NULL END) as avgDurationMinutes " +
            "FROM prod_task t LEFT JOIN prod_prescription p ON t.prescription_id = p.id " +
            "WHERE t.deleted = 0 AND DATE(t.complete_time) BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY DATE(t.complete_time) ORDER BY statDate DESC")
    List<Map<String, Object>> selectDailyCapacity(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
