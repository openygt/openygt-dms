package cn.org.openygt.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 质检检查项明细，对应表 {@code qt_inspection_item}。
 */
@Data
@TableName("qt_inspection_item")
public class InspectionItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inspectionId;
    private String itemCode;
    private String itemName;
    private String result;
    private String actualValue;
    private String remark;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
