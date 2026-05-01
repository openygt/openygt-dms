package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.PatientProgressDTO;
import cn.org.openygt.production.dto.PrescriptionTraceDTO;
import cn.org.openygt.production.entity.PatientToken;
import cn.org.openygt.production.entity.Prescription;

import java.util.List;

public interface PatientQueryService {
    PatientToken queryByCode(String token);
    List<Prescription> queryByPhone(String phone);
    PatientProgressDTO getProgress(String token);
    PrescriptionTraceDTO getTrace(Long prescriptionId);
}
