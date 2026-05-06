package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 质检趋势统计 DTO。
 */
@Data
public class InspectionTrendDTO implements Serializable {

    private String period;
    private Integer totalCount;
    private Integer passCount;
    private Integer concessionCount;
    private Integer reworkCount;
    private Integer scrapCount;

    public Integer getTotal() {
        return totalCount;
    }
}
