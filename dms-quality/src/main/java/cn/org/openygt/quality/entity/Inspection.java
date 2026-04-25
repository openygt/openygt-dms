package cn.org.openygt.quality.entity;

import cn.org.openygt.common.entity.BaseEntity;
import cn.org.openygt.common.enums.InspectionResultType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 质检记录实体，对应表 {@code qt_inspection}。
 *
 * <p>inspected_at 为业务质检时间，与 created_at 解耦（支持草稿模式）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qt_inspection")
public class Inspection extends BaseEntity {

    private Long taskId;
    private InspectionResultType result;
    private String operatorId;
    private String remark;
    private LocalDateTime inspectedAt;
}
