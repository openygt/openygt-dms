package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.EmergencyPrescriptionDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface EmergencyPrescriptionService {

    IPage<EmergencyPrescriptionDTO> listEmergencyPrescriptions(Integer emergencyLevel, String status, int page, int size);

    EmergencyPrescriptionDTO markEmergency(Long prescriptionId, Integer emergencyLevel);

    EmergencyPrescriptionDTO getEmergencyDetail(Long prescriptionId);

    EmergencyPrescriptionDTO signEmergency(Long emergencyId, String nurseName);
}
