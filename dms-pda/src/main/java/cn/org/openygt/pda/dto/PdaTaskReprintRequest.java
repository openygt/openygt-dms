package cn.org.openygt.pda.dto;

import lombok.Data;

@Data
public class PdaTaskReprintRequest {

    private String printType;
    private Integer printCount;
}
