package cn.org.openygt.quality.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface QcRateMapper {

    @Select("<script>"
            + "SELECT DATE_FORMAT(inspected_at, #{format}) as period, "
            + "COUNT(*) as total, SUM(CASE WHEN result = 'PASS' THEN 1 ELSE 0 END) as passCount, "
            + "ROUND(SUM(CASE WHEN result = 'PASS' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as rate "
            + "FROM qt_inspection WHERE deleted = 0"
            + "<if test='dateStart != null and dateStart !=\"\"'> AND inspected_at &gt;= #{dateStart}</if>"
            + "<if test='dateEnd != null and dateEnd !=\"\"'> AND inspected_at &lt;= #{dateEnd}</if>"
            + " GROUP BY DATE_FORMAT(inspected_at, #{format}) ORDER BY period"
            + "</script>")
    List<Map<String, Object>> trend(@Param("dateStart") String dateStart, @Param("dateEnd") String dateEnd,
                                     @Param("format") String format);

    @Select("<script>"
            + "SELECT i.item_name as item, "
            + "SUM(CASE WHEN i.result = 'PASS' THEN 1 ELSE 0 END) as pass, "
            + "SUM(CASE WHEN i.result = 'FAIL' THEN 1 ELSE 0 END) as fail "
            + "FROM qt_inspection_item i "
            + "INNER JOIN qt_inspection q ON i.inspection_id = q.id "
            + "WHERE q.deleted = 0"
            + "<if test='dateStart != null and dateStart !=\"\"'> AND q.inspected_at &gt;= #{dateStart}</if>"
            + "<if test='dateEnd != null and dateEnd !=\"\"'> AND q.inspected_at &lt;= #{dateEnd}</if>"
            + " GROUP BY i.item_code, i.item_name"
            + " ORDER BY fail DESC"
            + "</script>")
    List<Map<String, Object>> reasonStat(@Param("dateStart") String dateStart, @Param("dateEnd") String dateEnd);

    @Select("<script>"
            + "SELECT 'ALL' as name, COUNT(*) as total, SUM(CASE WHEN result = 'PASS' THEN 1 ELSE 0 END) as passCount FROM qt_inspection WHERE deleted = 0"
            + "<if test='dateStart != null and dateStart !=\"\"'> AND inspected_at &gt;= #{dateStart}</if>"
            + "<if test='dateEnd != null and dateEnd !=\"\"'> AND inspected_at &lt;= #{dateEnd}</if>"
            + "</script>")
    Map<String, Object> summary(@Param("dateStart") String dateStart, @Param("dateEnd") String dateEnd);
}
