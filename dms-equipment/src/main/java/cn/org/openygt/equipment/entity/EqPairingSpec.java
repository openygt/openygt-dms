package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_pairing_spec")
public class EqPairingSpec extends BaseEntity {

    private String specCode;
    private String specName;
    private Integer decocterCount;
    private Integer packerCount;
    private Integer printerCount;
    private String description;
    private String status;
}
