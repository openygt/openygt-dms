package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("prod_work_record")
public class WorkRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String operatorId;
    private String operatorName;
    private Long taskId;
    private String action;
    private Integer workTime;
    @TableLogic
    private Integer deleted;
    private Date createdAt;
    private Date updatedAt;
}
