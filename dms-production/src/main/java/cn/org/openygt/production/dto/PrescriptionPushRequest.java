package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class PrescriptionPushRequest {

    /** HIS系统标识 */
    @NotBlank
    private String hospitalCode;

    /** 推送批次号（用于幂等） */
    private String batchNo;

    @NotEmpty
    @Valid
    private List<PushPrescription> prescriptions;

    @Data
    public static class PushPrescription {
        @NotBlank
        private String prescriptionNo;
        @NotBlank
        private String patientName;
        private Integer patientType;
        private String doctorName;
        private String department;
        private String disease;
        private Integer repetition;
        private String usageMethod;
        private String remark;

        @NotEmpty
        @Valid
        private List<PushMedicineItem> items;
    }

    @Data
    public static class PushMedicineItem {
        /** HIS端药品编码（用于映射匹配） */
        private String hisMedicineCode;
        private String hisMedicineName;
        private String dosage;
        private String unit;
        private String medUsage;
        private String decoctionMethod;
    }
}
