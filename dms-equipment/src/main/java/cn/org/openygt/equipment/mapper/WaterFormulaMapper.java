package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.WaterFormula;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WaterFormulaMapper extends BaseMapper<WaterFormula> {

    @Select("SELECT * FROM water_formula WHERE formula_code = #{formulaCode} AND deleted = 0")
    WaterFormula findByCode(@Param("formulaCode") String formulaCode);

    @Select("SELECT * FROM water_formula WHERE is_default = 1 AND enabled = 1 AND deleted = 0 LIMIT 1")
    WaterFormula findDefault();
}
