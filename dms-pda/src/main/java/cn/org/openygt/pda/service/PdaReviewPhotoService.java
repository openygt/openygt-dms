package cn.org.openygt.pda.service;

import cn.org.openygt.pda.entity.PdaReviewPhoto;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PdaReviewPhotoService extends IService<PdaReviewPhoto> {

    PdaReviewPhoto uploadPhoto(Long taskId, Long prescriptionId, String photoUrl,
                               String photoType, Long fileSize, Long operatorId,
                               String operatorName, String remark);

    List<PdaReviewPhoto> listByTaskId(Long taskId);

    List<PdaReviewPhoto> listByPrescriptionId(Long prescriptionId);

    Long countByTaskIdAndType(Long taskId, String photoType);
}
