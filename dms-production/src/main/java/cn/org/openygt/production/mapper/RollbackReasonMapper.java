package cn.org.openygt.production.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.org.openygt.production.entity.RollbackReason;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RollbackReasonMapper extends BaseMapper<RollbackReason> {
}
