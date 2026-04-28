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
public class DeviceConnInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceCode;
    private String protocol;
    private String status;
    private LocalDateTime lastHeartbeat;
    private Long tenantId;
    private Long groupId;
    private String adapterType;
    private String ipAddress;
    private LocalDateTime connectedAt;
}
