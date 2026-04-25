package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.PrescriptionCreateRequest;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.service.PrescriptionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper prescriptionMapper;
    private final TaskMapper taskMapper;

    @Override
    @Transactional
    public Prescription create(PrescriptionCreateRequest request) {
        Prescription prescription = new Prescription();
        prescription.setPatientName(request.getPatientName());
        prescription.setMedicineList(request.getMedicineList());
        prescription.setRemark(request.getRemark());
        prescription.setReceiveTime(new Date());
        prescriptionMapper.insert(prescription);

        Task task = new Task();
        task.setPrescriptionId(prescription.getId());
        task.setStatus("待泡药");
        task.setTargetTemp(BigDecimal.valueOf(100));
        task.setSoakDuration(30);
        taskMapper.insert(task);

        return prescription;
    }

    @Override
    public Prescription getById(Long id) {
        return prescriptionMapper.selectById(id);
    }

    @Override
    public IPage<Prescription> list(Long hospitalId, Integer patientType, int page, int size) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        if (hospitalId != null) {
            wrapper.eq(Prescription::getHospitalId, hospitalId);
        }
        if (patientType != null) {
            wrapper.eq(Prescription::getPatientType, patientType);
        }
        wrapper.orderByDesc(Prescription::getCreatedAt);
        return prescriptionMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
