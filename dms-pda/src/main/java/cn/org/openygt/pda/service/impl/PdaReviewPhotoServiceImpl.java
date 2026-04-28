package cn.org.openygt.pda.service.impl;

import cn.org.openygt.pda.entity.PdaReviewPhoto;
import cn.org.openygt.pda.mapper.PdaReviewPhotoMapper;
import cn.org.openygt.pda.service.PdaReviewPhotoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PdaReviewPhotoServiceImpl extends ServiceImpl<PdaReviewPhotoMapper, PdaReviewPhoto>
        implements PdaReviewPhotoService {

    @Override
    public PdaReviewPhoto uploadPhoto(Long taskId, Long prescriptionId, String photoUrl,
                                      String photoType, Long fileSize, Long operatorId,
                                      String operatorName, String remark) {
        PdaReviewPhoto photo = new PdaReviewPhoto();
        photo.setTaskId(taskId);
        photo.setPrescriptionId(prescriptionId);
        photo.setPhotoUrl(photoUrl);
        photo.setPhotoType(photoType);
        photo.setFileSize(fileSize);
        photo.setOperatorId(operatorId);
        photo.setOperatorName(operatorName);
        photo.setReviewTime(LocalDateTime.now());
        photo.setRemark(remark);
        baseMapper.insert(photo);
        return photo;
    }

    @Override
    public List<PdaReviewPhoto> listByTaskId(Long taskId) {
        return baseMapper.selectByTaskId(taskId);
    }

    @Override
    public List<PdaReviewPhoto> listByPrescriptionId(Long prescriptionId) {
        return baseMapper.selectByPrescriptionId(prescriptionId);
    }

    @Override
    public Long countByTaskIdAndType(Long taskId, String photoType) {
        return baseMapper.countByTaskIdAndType(taskId, photoType);
    }
}
