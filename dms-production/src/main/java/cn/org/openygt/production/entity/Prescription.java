package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("prod_prescription")
public class Prescription {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long hospitalId;
    @TableField(exist = false)
    private String hospitalName;
    @TableField(exist = false)
    private String status;
    private String prescriptionNumber;

    public String getPrescriptionNo() { return prescriptionNumber; }
    public void setPrescriptionNo(String prescriptionNo) { this.prescriptionNumber = prescriptionNo; }
    private String patientName;
    private String patientPhone;
    private Integer patientType;
    private String outpatientNo;
    private String inpatientNo;
    private String bedNo;
    private String disease;
    private String doctorName;
    private String department;
    private String diseaseArea;
    private String medicineList;
    private Integer repetition;
    private Integer bagsPerRepetition;
    private Integer bagCapacity;
    private Integer decoctingType;
    private String deliveryType;
    private String preparationType;
    private String usageMethod;
    private Long schemeId;
    private String remark;
    private Date receiveTime;
    @TableLogic
    private Integer deleted;
    private Date createdAt;
    private Date updatedAt;

    private String receiveStatus;
    private String rejectType;
    private String rejectReason;
    private Date receivedAt;
    private Date rejectedAt;
    private String taskNo;
    private Long operatorId;
    private String operatorName;
    @TableField(exist = false)
    private Integer doseCount;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private List<PrescriptionMedicine> medicineItems;

    /** V34: 导入异常标识 0=正常 1=异常待处理 */
    private Integer importException;
    /** V34: 异常原因 */
    private String exceptionReason;
    /** V34: 原始导入数据(JSON) */
    private String rawImportData;
    /** V34: 处方来源（接口导入/手工录入/CSV导入） */
    private String source;
}
