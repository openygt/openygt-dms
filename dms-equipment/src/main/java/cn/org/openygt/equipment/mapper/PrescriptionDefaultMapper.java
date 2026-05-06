package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.PrescriptionDefault;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PrescriptionDefaultMapper extends BaseMapper<PrescriptionDefault> {

    @Select("SELECT * FROM prod_prescription_default WHERE setting_key = #{key} AND deleted = 0")
    PrescriptionDefault findByKey(@Param("key") String key);
}
