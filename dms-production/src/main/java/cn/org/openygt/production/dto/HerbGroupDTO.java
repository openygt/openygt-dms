package cn.org.openygt.production.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HerbGroupDTO {
    private Long id;
    private Long prescriptionId;
    private String groupCode;
    private String groupName;
    private String groupColor;
    private String groupIcon;
    private Integer groupSeq;
    private Integer processStatus;
    private LocalDateTime processTime;
    private Long operatorId;
    private Long deviceId;
    private String processType;
    private String specialInstruction;
    private List<HerbItemDTO> herbs;

    @Data
    public static class HerbItemDTO {
        private String herbName;
        private String dosage;
        private String unit;
    }
}
