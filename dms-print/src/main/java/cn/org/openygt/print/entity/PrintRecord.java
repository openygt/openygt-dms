package cn.org.openygt.print.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prt_record")
public class PrintRecord {

    private Long id;

    private String tenantId;

    private Long printTaskId;

    private String result;

    private String errorMessage;

    /** 实际执行打印的打印机编码 */
    private String printerCode;

    /** 触发本次打印/重试的操作人 ID */
    private String operatorId;

    private LocalDateTime printedAt;

    private Integer deleted;
}
