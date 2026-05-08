package cn.org.openygt.masterdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.org.openygt.masterdata.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper

public interface MdMedicineMapper extends BaseMapper<Medicine> {
}
