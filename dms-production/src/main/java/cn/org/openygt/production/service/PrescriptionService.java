package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.PrescriptionCreateRequest;
import cn.org.openygt.production.entity.Prescription;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface PrescriptionService {
    Prescription create(PrescriptionCreateRequest request);
    Prescription getById(Long id);
    IPage<Prescription> list(Long hospitalId, Integer patientType, String status, String keyword, String startTime, String endTime, int page, int size);
    Prescription receive(Long id, Long operatorId, String operatorName);
    Prescription reject(Long id, String rejectType, String reason, Long operatorId, String operatorName);
    Prescription getDetail(Long id);
}
