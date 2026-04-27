package cn.org.openygt.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inv_stock_log")
public class InvStockLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long medicineId;
    private String medicineCode;
    private String medicineName;
    private Long batchId;
    private String changeType;
    private BigDecimal changeQuantity;
    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private String refNo;
    private String operatorId;
    private String remark;
    private String tenantId;
    private LocalDateTime createdAt;
}
