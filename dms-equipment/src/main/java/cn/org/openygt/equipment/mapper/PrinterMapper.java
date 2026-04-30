package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.EqDevice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrinterMapper extends BaseMapper<EqDevice> {

    @Select("<script>"
            + "SELECT * FROM eq_device WHERE deleted = 0 AND device_type IN (3, 4)"
            + "<if test='keyword != null and keyword !=\"\"'> AND (name LIKE CONCAT('%',#{keyword},'%') OR device_code LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "<if test='deviceType != null'> AND device_type = #{deviceType}</if>"
            + " ORDER BY created_at DESC LIMIT #{offset},#{size}"
            + "</script>")
    List<EqDevice> listPrinters(@Param("keyword") String keyword, @Param("deviceType") Integer deviceType,
                                 @Param("offset") int offset, @Param("size") int size);

    @Select("<script>"
            + "SELECT COUNT(*) FROM eq_device WHERE deleted = 0 AND device_type IN (3, 4)"
            + "<if test='keyword != null and keyword !=\"\"'> AND (name LIKE CONCAT('%',#{keyword},'%') OR device_code LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "<if test='deviceType != null'> AND device_type = #{deviceType}</if>"
            + "</script>")
    long listPrintersCount(@Param("keyword") String keyword, @Param("deviceType") Integer deviceType);
}
