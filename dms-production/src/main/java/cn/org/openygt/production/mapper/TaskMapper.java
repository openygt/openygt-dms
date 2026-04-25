package cn.org.openygt.production.mapper;

import cn.org.openygt.production.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 悲观锁查询 —— MySQL 迁移后生效（SQLite 不支持 FOR UPDATE）。
     */
    @Select("SELECT * FROM prod_task WHERE id = #{id} AND deleted = 0")
    Task selectByIdForUpdate(@Param("id") Long id);
}
