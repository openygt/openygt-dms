package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("prod_handover_detail")
public class HandoverDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer bagCount;
    private String handoverType;
    private String handoverUser;
    private Date handoverTime;
    private String remark;
    private Date createdAt;
}
