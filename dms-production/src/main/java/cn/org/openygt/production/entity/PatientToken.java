package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_patient_token")
public class PatientToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String token;
    private Long prescriptionId;
    private String patientPhone;
    private LocalDateTime expireAt;
    private Integer accessCount;
    private LocalDateTime createdAt;
}
