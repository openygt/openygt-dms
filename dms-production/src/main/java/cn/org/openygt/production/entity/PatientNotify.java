package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_patient_notify")
public class PatientNotify {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prescriptionId;
    private String notifyType;
    private String channel;
    private String content;
    private Integer isSent;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
