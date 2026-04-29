package cn.org.openygt.production.dto;

import cn.org.openygt.common.enums.InspectionResultType;
import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class QualityInspectRequest {
    @NotNull
    private InspectionResultType result;
    private String operatorId;
    private String remark;
    private String reworkNode;
}
