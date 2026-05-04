package cn.org.openygt.production.mapper;

import cn.org.openygt.production.entity.HrEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HrEmployeeMapper {

    @Select({"<script>",
             "SELECT id, real_name FROM sys_user WHERE id IN ",
             "<foreach item='item' index='index' collection='employeeIds' open='(' separator=',' close=')'>#{item}</foreach>",
             "</script>"})
    List<HrEmployee> findByIds(@Param("employeeIds") List<Long> employeeIds);
}
