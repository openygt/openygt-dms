package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.dto.PrintLogDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrintLogMapper {

    @Select("<script>"
            + "SELECT t.id, t.task_id, t.device_code, t.operator_id, t.status, t.copies, t.retry_count, t.max_retry, "
            + "r.result, r.error_message, t.device_code as printer_code, r.printed_at, t.created_at "
            + "FROM prt_task t LEFT JOIN prt_record r ON r.print_task_id = t.id "
            + "WHERE t.deleted = 0 "
            + "<if test='status != null'> AND t.status = #{status} </if>"
            + "<if test='deviceCode != null'> AND t.device_code LIKE CONCAT('%', #{deviceCode}, '%') </if>"
            + "ORDER BY t.created_at DESC"
            + "</script>")
    List<PrintLogDTO> listPrintLogs(@Param("status") String status,
                                    @Param("deviceCode") String deviceCode);
}
