package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dms_employee_barcode")
public class EmployeeBarcode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private String barcode;
    private String barcodeType;
    private String cardType;
    private Integer printCount;
    private LocalDateTime lastPrintTime;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer isActive;
    private LocalDateTime createdAt;
}
