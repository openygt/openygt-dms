package cn.org.openygt.production.mapper;

import cn.org.openygt.production.entity.PrescriptionMedicine;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrescriptionMedicineMapper extends BaseMapper<PrescriptionMedicine> {
    @Select("SELECT * FROM prod_prescription_medicine WHERE prescription_id = #{prescriptionId} AND deleted = 0 ORDER BY sort_order")
    List<PrescriptionMedicine> selectByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}
