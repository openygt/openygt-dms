package cn.org.openygt.print.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prt_task")
public class PrintTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private Long taskId;

    private String deviceCode;

    private String operatorId;

    private String status;

    private Integer copies;

    private Integer retryCount;

    private Integer maxRetry;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;
}
