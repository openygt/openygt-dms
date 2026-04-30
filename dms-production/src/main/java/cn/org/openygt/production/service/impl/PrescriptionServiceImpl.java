package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.PrescriptionCreateRequest;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.entity.PrescriptionMedicine;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.mapper.PrescriptionMedicineMapper;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper prescriptionMapper;
    private final TaskMapper taskMapper;
    private final PrescriptionMedicineMapper prescriptionMedicineMapper;

    @Override
    @Transactional
    public Prescription create(PrescriptionCreateRequest request) {
        Prescription prescription = new Prescription();
        prescription.setPatientName(request.getPatientName());
        prescription.setMedicineList(request.getMedicineList());
        prescription.setRemark(request.getRemark());
        prescription.setReceiveTime(new Date());
        prescription.setReceiveStatus("PENDING");
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
    public IPage<Prescription> list(Long hospitalId, Integer patientType, String status, String keyword, String startTime, String endTime, int page, int size) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        if (hospitalId != null) {
            wrapper.eq(Prescription::getHospitalId, hospitalId);
        }
        if (patientType != null) {
            wrapper.eq(Prescription::getPatientType, patientType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Prescription::getPatientName, keyword)
                    .or().like(Prescription::getPatientPhone, keyword));
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(Prescription::getCreatedAt, startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(Prescription::getCreatedAt, endTime + " 23:59:59");
        }

        boolean isReceiveStatus = "PENDING".equals(status) || "RECEIVED".equals(status) || "REJECTED".equals(status);
        if (isReceiveStatus) {
            wrapper.eq(Prescription::getReceiveStatus, status);
        }

        wrapper.orderByDesc(Prescription::getCreatedAt);

        List<Prescription> allList = prescriptionMapper.selectList(wrapper);
        if (allList.isEmpty()) {
            return new Page<>(page, size);
        }

        // 批量查询关联任务
        Set<Long> prescriptionIds = allList.stream().map(Prescription::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(Task::getPrescriptionId, prescriptionIds);
        List<Task> tasks = taskMapper.selectList(taskWrapper);
        Map<Long, List<Task>> taskMap = tasks.stream().collect(Collectors.groupingBy(Task::getPrescriptionId));

        // 计算状态并过滤
        List<Prescription> filtered = new ArrayList<>();
        for (Prescription p : allList) {
            p.setDoseCount(p.getRepetition());
            p.setDeptName(p.getDepartment());
            if (isReceiveStatus) {
                p.setStatus(p.getReceiveStatus());
                filtered.add(p);
            } else {
                String calcStatus = calcPrescriptionStatus(taskMap.get(p.getId()));
                p.setStatus(calcStatus);
                if (status == null || status.isEmpty() || status.equals(calcStatus)) {
                    filtered.add(p);
                }
            }
        }

        // 手动分页
        Page<Prescription> resultPage = new Page<>(page, size);
        resultPage.setTotal(filtered.size());
        int from = (page - 1) * size;
        if (from < filtered.size()) {
            int to = Math.min(from + size, filtered.size());
            resultPage.setRecords(filtered.subList(from, to));
        } else {
            resultPage.setRecords(Collections.emptyList());
        }
        return resultPage;
    }

    private String calcPrescriptionStatus(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "待处理";
        }
        boolean allPending = tasks.stream().allMatch(t -> "待泡药".equals(t.getStatus()));
        boolean allCompleted = tasks.stream().allMatch(t -> "已完成".equals(t.getStatus()));
        if (allPending) {
            return "待处理";
        }
        if (allCompleted) {
            return "处理完毕";
        }
        return "处理中";
    }

    @Override
    @Transactional
    public Prescription receive(Long id, Long operatorId, String operatorName) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new IllegalArgumentException("处方不存在");
        }
        if (!"PENDING".equals(prescription.getReceiveStatus())) {
            throw new IllegalArgumentException("处方状态不是待接收");
        }

        String taskNo = generateTaskNo();
        prescription.setReceiveStatus("RECEIVED");
        prescription.setTaskNo(taskNo);
        prescription.setReceivedAt(new Date());
        prescription.setOperatorId(operatorId);
        prescription.setOperatorName(operatorName);
        prescriptionMapper.updateById(prescription);

        Task task = new Task();
        task.setPrescriptionId(prescription.getId());
        task.setStatus("待泡药");
        task.setTargetTemp(BigDecimal.valueOf(100));
        task.setSoakDuration(30);
        if (prescription.getSchemeId() != null) {
            task.setSchemeId(prescription.getSchemeId());
        }
        taskMapper.insert(task);

        return prescription;
    }

    @Override
    @Transactional
    public Prescription reject(Long id, String rejectType, String reason, Long operatorId, String operatorName) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new IllegalArgumentException("处方不存在");
        }
        if (!"PENDING".equals(prescription.getReceiveStatus())) {
            throw new IllegalArgumentException("处方状态不是待接收");
        }

        prescription.setReceiveStatus("REJECTED");
        prescription.setRejectType(rejectType);
        prescription.setRejectReason(reason);
        prescription.setRejectedAt(new Date());
        prescription.setOperatorId(operatorId);
        prescription.setOperatorName(operatorName);
        prescriptionMapper.updateById(prescription);

        return prescription;
    }

    @Override
    public Prescription getDetail(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            return null;
        }
        prescription.setDoseCount(prescription.getRepetition());
        prescription.setDeptName(prescription.getDepartment());
        List<PrescriptionMedicine> medicines = prescriptionMedicineMapper.selectByPrescriptionId(id);
        prescription.setMedicineItems(medicines);
        return prescription;
    }

    private String generateTaskNo() {
        String prefix = "D" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Prescription::getTaskNo, prefix)
                .orderByDesc(Prescription::getId)
                .last("LIMIT 1");
        Prescription latest = prescriptionMapper.selectOne(wrapper);
        if (latest != null && latest.getTaskNo() != null && latest.getTaskNo().startsWith(prefix)) {
            try {
                int seq = Integer.parseInt(latest.getTaskNo().substring(prefix.length())) + 1;
                return prefix + String.format("%04d", seq);
            } catch (NumberFormatException e) {
                return prefix + "0001";
            }
        }
        return prefix + "0001";
    }
}
