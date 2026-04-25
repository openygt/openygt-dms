package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_handover_detail")
public class HandoverDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer bagCount;
    private String handoverType;
    private String handoverUser;
    private LocalDateTime handoverTime;
    private String remark;
    private LocalDateTime createdAt;
}
