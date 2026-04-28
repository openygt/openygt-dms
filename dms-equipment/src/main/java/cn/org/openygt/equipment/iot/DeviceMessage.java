package cn.org.openygt.equipment.iot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceCode;
    private String messageType;
    private String protocol;
    private Object payload;
    private LocalDateTime timestamp;
    private Long tenantId;
}
