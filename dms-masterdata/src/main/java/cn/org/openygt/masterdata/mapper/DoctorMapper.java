package cn.org.openygt.masterdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.org.openygt.masterdata.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {
}
