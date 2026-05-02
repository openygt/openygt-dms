package cn.org.openygt.production.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class OcrPrescriptionRequest {

    /** OCR识别后的JSON结果 */
    @NotBlank
    private String patientName;

    private String doctorName;
    private String department;
    private String disease;
    private Integer repetition;

    @NotEmpty
    private List<OcrMedicineItem> items;

    /** OCR源图片URL/路径 */
    private String imageUrl;

    @Data
    public static class OcrMedicineItem {
        @NotBlank
        private String medicineName;
        private String dosage;
        private String unit;
        private String medUsage;
    }
}
