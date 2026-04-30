package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_doctor")
public class Doctor extends BaseEntity {

    private String doctorCode;
    private String doctorName;
    private String title;
    private Long departmentId;
    private Long hospitalId;
    private String phone;
    private Integer status;
}
