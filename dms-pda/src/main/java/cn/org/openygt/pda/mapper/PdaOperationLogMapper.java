package cn.org.openygt.pda.mapper;

import cn.org.openygt.pda.entity.PdaOperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PdaOperationLogMapper extends BaseMapper<PdaOperationLog> {

    @Select("SELECT * FROM pda_operation_log WHERE user_id = #{userId} ORDER BY oper_time DESC LIMIT #{limit}")
    List<PdaOperationLog> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT * FROM pda_operation_log WHERE task_id = #{taskId} ORDER BY oper_time DESC")
    List<PdaOperationLog> selectByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM pda_operation_log WHERE oper_type = #{operType} AND oper_time BETWEEN #{start} AND #{end} ORDER BY oper_time DESC")
    List<PdaOperationLog> selectByTypeAndTimeRange(@Param("operType") String operType,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);
}
