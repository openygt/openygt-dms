package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 质检汇总统计 DTO。
 */
@Data
public class InspectionSummaryDTO implements Serializable {

    private Integer totalCount;
    private Integer passCount;
    private Integer concessionCount;
    private Integer reworkCount;
    private Integer scrapCount;
    private Double passRate;
    private Double reworkRate;
    private Double scrapRate;
}
