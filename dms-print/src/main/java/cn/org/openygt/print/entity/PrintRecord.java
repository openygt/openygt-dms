package cn.org.openygt.print.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("prt_record")
public class PrintRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private Long printTaskId;

    private String result;

    private String errorMessage;

    private Date printedAt;

    private Integer deleted;
}
