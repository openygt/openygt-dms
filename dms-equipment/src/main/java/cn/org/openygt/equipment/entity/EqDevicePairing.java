package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_device_pairing")
public class EqDevicePairing extends BaseEntity {

    private String pairingName;
    private String decocterIds;
    private Long packerId;
    private Long labelerId;
    private String status;
}
