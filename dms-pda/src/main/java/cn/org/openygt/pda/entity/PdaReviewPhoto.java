package cn.org.openygt.pda.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pda_review_photo")
public class PdaReviewPhoto extends cn.org.openygt.common.entity.BaseEntity {

    private Long taskId;
    private Long prescriptionId;
    private String photoUrl;
    private String thumbnailUrl;
    private String photoType;
    private Long fileSize;
    private Long width;
    private Long height;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime reviewTime;
    private String remark;
}
