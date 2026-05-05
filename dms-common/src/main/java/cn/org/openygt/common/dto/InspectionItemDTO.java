package cn.org.openygt.common.dto;

import lombok.Data;

/**
 * 质检检查项明细 DTO（跨模块 SPI 传输用）。
 */
@Data
public class InspectionItemDTO {

    private String itemCode;
    private String itemName;
    private String result;
    private String actualValue;
    private String remark;
}
