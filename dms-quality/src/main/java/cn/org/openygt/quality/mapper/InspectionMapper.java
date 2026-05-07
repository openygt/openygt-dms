package cn.org.openygt.quality.mapper;

import cn.org.openygt.quality.entity.Inspection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InspectionMapper extends BaseMapper<Inspection> {

    @Select("<script>"
            + "SELECT result, COUNT(*) AS cnt FROM qt_inspection WHERE deleted = 0"
            + "<if test='from != null'> AND inspected_at &gt;= #{from}</if>"
            + "<if test='to != null'> AND inspected_at &lt;= #{to}</if>"
            + " GROUP BY result"
            + "</script>")
    List<Map<String, Object>> countByResult(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
