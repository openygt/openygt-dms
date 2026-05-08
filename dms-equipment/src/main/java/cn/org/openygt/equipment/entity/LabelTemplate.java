package cn.org.openygt.equipment.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prt_label_template")
public class LabelTemplate extends BaseEntity {

    private String templateCode;
    private String templateName;
    private String templateType;
    private Integer widthMm;
    private Integer heightMm;
    private String content;
    private String previewImage;
    private Integer status;
}
