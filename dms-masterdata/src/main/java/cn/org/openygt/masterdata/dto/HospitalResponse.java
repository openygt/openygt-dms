package cn.org.openygt.masterdata.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医院信息响应 DTO。
 *
 * <p>屏蔽内部字段（deleted、tenantId 等），仅暴露前端需要的数据。</p>
 */
@Data
public class HospitalResponse implements Serializable {

    private Long id;
    private String hospitalName;
    private String hospitalCode;
    private String contactName;
    private String contactPhone;
    private String address;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
