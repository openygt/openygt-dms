package cn.org.openygt.equipment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExceptionTraceMapper {

    @Select("<script>"
            + "SELECT * FROM prod_exception_log WHERE is_deleted = 0"
            + "<if test='keyword != null and keyword !=\"\"'> AND (patient_name LIKE CONCAT('%',#{keyword},'%') OR prescription_no LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "<if test='exceptionType != null and exceptionType !=\"\"'> AND exception_type = #{exceptionType}</if>"
            + "<if test='handleStatus != null and handleStatus !=\"\"'> AND handle_status = #{handleStatus}</if>"
            + "<if test='dateStart != null and dateStart !=\"\"'> AND created_at &gt;= #{dateStart}</if>"
            + "<if test='dateEnd != null and dateEnd !=\"\"'> AND created_at &lt;= #{dateEnd}</if>"
            + " ORDER BY created_at DESC LIMIT #{offset},#{size}"
            + "</script>")
    List<Map<String, Object>> list(@Param("keyword") String keyword, @Param("exceptionType") String exceptionType,
                                    @Param("handleStatus") String handleStatus, @Param("dateStart") String dateStart,
                                    @Param("dateEnd") String dateEnd, @Param("offset") int offset, @Param("size") int size);

    @Select("<script>"
            + "SELECT COUNT(*) FROM prod_exception_log WHERE is_deleted = 0"
            + "<if test='keyword != null and keyword !=\"\"'> AND (patient_name LIKE CONCAT('%',#{keyword},'%') OR prescription_no LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "<if test='exceptionType != null and exceptionType !=\"\"'> AND exception_type = #{exceptionType}</if>"
            + "<if test='handleStatus != null and handleStatus !=\"\"'> AND handle_status = #{handleStatus}</if>"
            + "<if test='dateStart != null and dateStart !=\"\"'> AND created_at &gt;= #{dateStart}</if>"
            + "<if test='dateEnd != null and dateEnd !=\"\"'> AND created_at &lt;= #{dateEnd}</if>"
            + "</script>")
    long listCount(@Param("keyword") String keyword, @Param("exceptionType") String exceptionType,
                    @Param("handleStatus") String handleStatus, @Param("dateStart") String dateStart,
                    @Param("dateEnd") String dateEnd);

    @Select("SELECT * FROM prod_exception_log WHERE is_deleted = 0 AND id = #{id}")
    Map<String, Object> getById(@Param("id") Long id);

    @Select("<script>"
            + "SELECT exception_type as name, COUNT(*) as value FROM prod_exception_log WHERE is_deleted = 0 "
            + "<if test='dateStart != null and dateStart !=\"\"'> AND created_at &gt;= #{dateStart}</if>"
            + "<if test='dateEnd != null and dateEnd !=\"\"'> AND created_at &lt;= #{dateEnd}</if>"
            + " GROUP BY exception_type"
            + "</script>")
    List<Map<String, Object>> statByType(@Param("dateStart") String dateStart, @Param("dateEnd") String dateEnd);

    @Select("<script>"
            + "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count FROM prod_exception_log WHERE is_deleted = 0 "
            + "<if test='dateStart != null and dateStart !=\"\"'> AND created_at &gt;= #{dateStart}</if>"
            + "<if test='dateEnd != null and dateEnd !=\"\"'> AND created_at &lt;= #{dateEnd}</if>"
            + " GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month"
            + "</script>")
    List<Map<String, Object>> statByMonth(@Param("dateStart") String dateStart, @Param("dateEnd") String dateEnd);
}
