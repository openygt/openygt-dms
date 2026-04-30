package cn.org.openygt.pda.dto;

import lombok.Data;

@Data
public class PdaTaskSignRequest {

    private String signType;
    private String signImageBase64;
    private String handoverFrom;
    private String handoverTo;
    private String remark;
}
