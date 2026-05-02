package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class PrescriptionStructuredCreateRequest {

    @NotBlank(message = "患者姓名不能为空")
    @Size(max = 100, message = "患者姓名长度不能超过100")
    private String patientName;

    private Long hospitalId;
    private Integer patientType;
    private String doctorName;
    private String department;
    private String disease;
    private Long schemeId;
    private Integer repetition;
    private Integer bagsPerRepetition;
    private Integer bagCapacity;
    private String usageMethod;
    private String remark;

    /** 来源：MANUAL/IMPORT/API/OCR */
    private String source;

    @NotEmpty(message = "药材明细不能为空")
    @Size(max = 200, message = "药材数量不能超过200")
    @Valid
    private List<PrescriptionMedicineItemRequest> medicineItems;
}
