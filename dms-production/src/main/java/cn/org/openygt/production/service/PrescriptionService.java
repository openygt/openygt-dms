package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.PrescriptionCreateRequest;
import cn.org.openygt.production.entity.Prescription;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface PrescriptionService {
    Prescription create(PrescriptionCreateRequest request);
    Prescription getById(Long id);
    IPage<Prescription> list(Long hospitalId, Integer patientType, int page, int size);
}
