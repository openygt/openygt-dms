package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prod_delivery_record")
public class DeliveryRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String prescriptionNo;
    private String patientName;
    private String deliveryType;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String courierCompany;
    private String courierNo;
    private Integer bagCount;
    private String status;
    private String operatorId;
    private String remark;
    private LocalDateTime deliveredAt;
    private String tenantId;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
