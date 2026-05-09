package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dms_herb_group_rule")
public class HerbGroupRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupCode;
    private String groupName;
    private String groupColor;
    private String groupIcon;
    private String processType;
    private Integer standardDuration;
    private String specialInstruction;
    private Integer sortOrder;
    private Integer isActive;
    private LocalDateTime createdAt;
}
