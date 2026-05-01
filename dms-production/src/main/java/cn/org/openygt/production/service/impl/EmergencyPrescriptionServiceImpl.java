package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.EmergencyPrescriptionDTO;
import cn.org.openygt.production.entity.EmergencyPrescription;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.mapper.EmergencyPrescriptionMapper;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.service.EmergencyPrescriptionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyPrescriptionServiceImpl implements EmergencyPrescriptionService {

    private final EmergencyPrescriptionMapper emergencyPrescriptionMapper;
    private final PrescriptionMapper prescriptionMapper;

    @Override
    public IPage<EmergencyPrescriptionDTO> listEmergencyPrescriptions(Integer emergencyLevel, String status, int page, int size) {
        LambdaQueryWrapper<EmergencyPrescription> wrapper = new LambdaQueryWrapper<>();
        if (emergencyLevel != null) {
            wrapper.eq(EmergencyPrescription::getEmergencyLevel, emergencyLevel);
        }
        wrapper.orderByDesc(EmergencyPrescription::getRequestTime);

        IPage<EmergencyPrescription> emergencyPage = emergencyPrescriptionMapper.selectPage(new Page<>(page, size), wrapper);

        if (emergencyPage.getRecords().isEmpty()) {
            Page<EmergencyPrescriptionDTO> emptyPage = new Page<>(page, size);
            emptyPage.setTotal(emergencyPage.getTotal());
            return emptyPage;
        }

        Set<Long> prescriptionIds = emergencyPage.getRecords().stream()
                .map(EmergencyPrescription::getPrescriptionId)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<Prescription> presWrapper = new LambdaQueryWrapper<>();
        presWrapper.in(Prescription::getId, prescriptionIds);
        List<Prescription> prescriptions = prescriptionMapper.selectList(presWrapper);
        Map<Long, Prescription> presMap = prescriptions.stream()
                .collect(Collectors.toMap(Prescription::getId, p -> p, (a, b) -> a));

        List<EmergencyPrescriptionDTO> dtoList = emergencyPage.getRecords().stream()
                .map(e -> convertToDTO(e, presMap.get(e.getPrescriptionId())))
                .filter(dto -> {
                    if (status == null || status.isEmpty()) return true;
                    return status.equals(dto.getStatus());
                })
                .collect(Collectors.toList());

        Page<EmergencyPrescriptionDTO> resultPage = new Page<>(page, size);
        resultPage.setTotal(emergencyPage.getTotal());
        resultPage.setRecords(dtoList);
        return resultPage;
    }

    @Override
    @Transactional
    public EmergencyPrescriptionDTO markEmergency(Long prescriptionId, Integer emergencyLevel) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new IllegalArgumentException("处方不存在");
        }

        LambdaQueryWrapper<EmergencyPrescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmergencyPrescription::getPrescriptionId, prescriptionId)
                .orderByDesc(EmergencyPrescription::getCreatedAt)
                .last("LIMIT 1");
        EmergencyPrescription existing = emergencyPrescriptionMapper.selectOne(wrapper);

        EmergencyPrescription emergency;
        if (existing != null) {
            emergency = existing;
            emergency.setEmergencyLevel(emergencyLevel);
            emergency.setUpdatedAt(LocalDateTime.now());
            emergencyPrescriptionMapper.updateById(emergency);
        } else {
            emergency = new EmergencyPrescription();
            emergency.setPrescriptionId(prescriptionId);
            emergency.setEmergencyLevel(emergencyLevel);
            emergency.setRequestTime(LocalDateTime.now());
            emergency.setPromisedFinishTime(LocalDateTime.now().plusMinutes(30));
            emergency.setCreatedAt(LocalDateTime.now());
            emergency.setUpdatedAt(LocalDateTime.now());
            emergencyPrescriptionMapper.insert(emergency);
        }

        return convertToDTO(emergency, prescription);
    }

    @Override
    public EmergencyPrescriptionDTO getEmergencyDetail(Long prescriptionId) {
        LambdaQueryWrapper<EmergencyPrescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmergencyPrescription::getPrescriptionId, prescriptionId)
                .orderByDesc(EmergencyPrescription::getCreatedAt)
                .last("LIMIT 1");
        EmergencyPrescription emergency = emergencyPrescriptionMapper.selectOne(wrapper);
        if (emergency == null) {
            return null;
        }
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        return convertToDTO(emergency, prescription);
    }

    @Override
    @Transactional
    public EmergencyPrescriptionDTO signEmergency(Long emergencyId, String nurseName) {
        EmergencyPrescription emergency = emergencyPrescriptionMapper.selectById(emergencyId);
        if (emergency == null) {
            throw new IllegalArgumentException("急诊记录不存在");
        }
        if (emergency.getNurseSignTime() != null) {
            throw new IllegalStateException("该急诊记录已签收");
        }

        emergency.setNurseName(nurseName);
        emergency.setNurseSignTime(LocalDateTime.now());
        emergency.setActualFinishTime(LocalDateTime.now());
        if (emergency.getPromisedFinishTime() != null) {
            emergency.setIsOnTime(emergency.getActualFinishTime().isBefore(emergency.getPromisedFinishTime()) ? 1 : 0);
        }
        emergency.setUpdatedAt(LocalDateTime.now());
        emergencyPrescriptionMapper.updateById(emergency);

        Prescription prescription = prescriptionMapper.selectById(emergency.getPrescriptionId());
        return convertToDTO(emergency, prescription);
    }

    private EmergencyPrescriptionDTO convertToDTO(EmergencyPrescription emergency, Prescription prescription) {
        EmergencyPrescriptionDTO dto = new EmergencyPrescriptionDTO();
        dto.setId(emergency.getId());
        dto.setPrescriptionId(emergency.getPrescriptionId());
        dto.setEmergencyLevel(emergency.getEmergencyLevel());
        dto.setRequestTime(emergency.getRequestTime());
        dto.setPromisedFinishTime(emergency.getPromisedFinishTime());
        dto.setActualFinishTime(emergency.getActualFinishTime());
        dto.setIsOnTime(emergency.getIsOnTime());
        dto.setDelayReason(emergency.getDelayReason());
        dto.setDeliveryType(emergency.getDeliveryType());
        dto.setDeliveryLocation(emergency.getDeliveryLocation());
        dto.setNurseName(emergency.getNurseName());
        dto.setNurseSignTime(emergency.getNurseSignTime());

        if (prescription != null) {
            dto.setPrescriptionNumber(prescription.getPrescriptionNumber());
            dto.setPatientName(prescription.getPatientName());
        }

        if (emergency.getNurseSignTime() != null) {
            dto.setStatus("SIGNED");
        } else if (emergency.getActualFinishTime() != null) {
            dto.setStatus("COMPLETED");
        } else {
            dto.setStatus("PENDING");
        }

        return dto;
    }
}
