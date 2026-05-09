package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_shelf_record")
public class ShelfRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prescriptionId;
    private String packageBarcode;
    private Long shelfId;
    private String shelfCode;
    private LocalDateTime putOnTime;
    private Long putOnBy;
    private LocalDateTime takeOffTime;
    private Long takeOffBy;
    private String takeOffType;
    private Integer status;
    private LocalDateTime expireWarningTime;
    private LocalDateTime createdAt;
}
