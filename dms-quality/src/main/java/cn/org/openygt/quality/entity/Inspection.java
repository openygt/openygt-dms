package cn.org.openygt.quality.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质检记录实体，对应表 {@code qt_inspection}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qt_inspection")
public class Inspection extends BaseEntity {

    private Long taskId;
    private String result;
    private String operatorId;
    private String remark;
}
