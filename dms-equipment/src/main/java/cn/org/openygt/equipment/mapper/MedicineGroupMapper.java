package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.MedicineGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicineGroupMapper extends BaseMapper<MedicineGroup> {

    @Select("SELECT * FROM md_medicine_group WHERE prescription_no = #{prescriptionNo} AND deleted = 0 ORDER BY group_type, sort_order")
    List<MedicineGroup> findByPrescriptionNo(@Param("prescriptionNo") String prescriptionNo);

    @Select("SELECT * FROM md_medicine_group WHERE prescription_no = #{prescriptionNo} AND group_type = #{groupType} AND deleted = 0 ORDER BY sort_order")
    List<MedicineGroup> findByPrescriptionAndType(@Param("prescriptionNo") String prescriptionNo, @Param("groupType") String groupType);
}
