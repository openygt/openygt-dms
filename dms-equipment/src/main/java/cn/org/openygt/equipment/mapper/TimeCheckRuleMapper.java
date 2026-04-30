package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.TimeCheckRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TimeCheckRuleMapper extends BaseMapper<TimeCheckRule> {

    @Select("SELECT * FROM time_check_rule WHERE to_step = #{toStep} AND enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<TimeCheckRule> findByToStep(@Param("toStep") String toStep);

    @Select("SELECT * FROM time_check_rule WHERE rule_code = #{ruleCode} AND deleted = 0")
    TimeCheckRule findByRuleCode(@Param("ruleCode") String ruleCode);
}
