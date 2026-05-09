package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dms_employee_skill")
public class EmployeeSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private String skillCode;
    private String skillName;
    private Integer proficiencyLevel;
    private LocalDateTime createdAt;
}
