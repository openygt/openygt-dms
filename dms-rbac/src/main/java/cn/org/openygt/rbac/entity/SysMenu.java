package cn.org.openygt.rbac.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 系统菜单实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    @NotBlank(message = "菜单编码不能为空")
    private String code;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer menuType; // 0=目录 1=菜单 2=按钮
    private Long parentId;
    private String status;
    private String permission; // 按钮权限标识
    private String description; // 页面功能描述

    @TableField(exist = false)
    private List<SysMenu> children;
}
