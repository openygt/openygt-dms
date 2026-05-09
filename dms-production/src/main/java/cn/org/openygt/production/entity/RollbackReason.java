package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_rollback_reason")
public class RollbackReason {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reasonCode;
    private String reasonName;
    private String reasonCategory;
    private Integer needApproval;
    private Integer approvalLevel;
    private Integer isActive;
    private LocalDateTime createdAt;
}
