package cn.org.openygt.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产异常记录表（prod_exception_log）实体类。
 *
 * <p>用于异常追溯模块，字段名采用驼峰以适配前端绑定；
 * 与数据库列名不一致处使用 {@link TableField} 显式映射。</p>
 */
@Data
@TableName("prod_exception_log")
public class ProdExceptionLog {

    private Long id;

    @TableField("exception_no")
    private String exceptionNo;

    @TableField("task_id")
    private Long taskId;

    @TableField("original_task_id")
    private Long originalTaskId;

    @TableField("new_task_id")
    private Long newTaskId;

    @TableField("prescription_no")
    private String prescriptionNo;

    @TableField("patient_name")
    private String patientName;

    @TableField("exception_type")
    private String exceptionType;

    @TableField("exception_level")
    private String exceptionLevel;

    @TableField("source_module")
    private String sourceModule;

    @TableField("current_step")
    private String currentStep;

    private String description;

    @TableField("discover_channel")
    private String discoverChannel;

    @TableField("discoverer_id")
    private String discovererId;

    @TableField("is_auto_handled")
    private Long isAutoHandled;

    @TableField("handle_status")
    private String handleStatus;

    @TableField("handler_id")
    private String handlerId;

    @TableField("handler_role")
    private String handlerRole;

    @TableField("assigned_at")
    private LocalDateTime assignedAt;

    private String resolution;

    @TableField("resolution_result")
    private String resolutionResult;

    @TableField("resolved_at")
    private LocalDateTime resolvedAt;

    @TableField("rework_target_step")
    private String reworkTargetStep;

    @TableField("rework_validate_result")
    private String reworkValidateResult;

    @TableField("loss_weight_gram")
    private Long lossWeightGram;

    @TableField("loss_amount_yuan")
    private BigDecimal lossAmountYuan;

    @TableField("waste_liquid_ml")
    private Long wasteLiquidMl;

    @TableField("device_no")
    private String deviceNo;

    @TableField("operator_id")
    private String operatorId;

    @TableField("shift_type")
    private String shiftType;

    @TableField("consumable_batch")
    private String consumableBatch;

    @TableField("root_cause_level1")
    private String rootCauseLevel1;

    @TableField("root_cause_level2")
    private String rootCauseLevel2;

    @TableField("root_cause")
    private String rootCause;

    @TableField("corrective_action")
    private String correctiveAction;

    @TableField("preventive_action")
    private String preventiveAction;

    @TableField("evidence_type")
    private String evidenceType;

    @TableField("evidence_barcode")
    private String evidenceBarcode;

    @TableField("evidence_urls")
    private String evidenceUrls;

    @TableField("handler_sign")
    private String handlerSign;

    @TableField("paper_record_no")
    private String paperRecordNo;

    @TableField("evidence_hash")
    private String evidenceHash;

    @TableField("sla_deadline")
    private LocalDateTime slaDeadline;

    @TableField("is_timeout")
    private Long isTimeout;

    @TableField("escalation_level")
    private Long escalationLevel;

    @TableField("escalated_at")
    private LocalDateTime escalatedAt;

    @TableField("is_concession")
    private Long isConcession;

    @TableField("doctor_sign")
    private String doctorSign;

    @TableField("doctor_sign2")
    private String doctorSign2;

    @TableField("patient_consent")
    private String patientConsent;

    @TableField("derived_workorder_id")
    private Long derivedWorkorderId;

    @TableField("derived_workorder_type")
    private String derivedWorkorderType;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("created_by")
    private String createdBy;

    @TableField("last_modified_at")
    private LocalDateTime lastModifiedAt;

    @TableField("last_modified_by")
    private String lastModifiedBy;

    @TableField("is_deleted")
    private Long isDeleted;

    // ---------- 前端友好别名（显式映射，避免 underscore-to-camel 偏差） ----------

    /**
     * 前端使用 handleResult 展示处理结果，实际对应数据库 resolution_result。
     */
    @TableField(exist = false)
    private String handleResult;

    /**
     * 前端使用 handledAt 展示处理时间，实际对应数据库 resolved_at。
     */
    @TableField(exist = false)
    private LocalDateTime handledAt;

    public String getHandleResult() {
        return this.resolutionResult;
    }

    public LocalDateTime getHandledAt() {
        return this.resolvedAt;
    }
}
