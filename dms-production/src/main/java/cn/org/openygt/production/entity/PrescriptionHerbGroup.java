package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dms_prescription_herb_group")
public class PrescriptionHerbGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prescriptionId;
    private String groupCode;
    private Integer groupSeq;
    private String herbsJson;
    private Integer processStatus;
    private LocalDateTime processTime;
    private Long operatorId;
    private Long deviceId;
    private LocalDateTime createdAt;
}
