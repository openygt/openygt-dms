package cn.org.openygt.rbac.mapper;

import cn.org.openygt.rbac.entity.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT m.* FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.status = 'ACTIVE' " +
            "ORDER BY m.sort_order")
    List<SysMenu> selectMenusByRoleId(@Param("roleId") Long roleId);

    @Select("<script>SELECT m.* FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id IN " +
            "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND m.status = 'ACTIVE' " +
            "ORDER BY m.sort_order</script>")
    List<SysMenu> selectMenusByRoleIds(@Param("roleIds") List<Long> roleIds);
}
