package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.PatientProgressDTO;
import cn.org.openygt.production.dto.PrescriptionTraceDTO;
import cn.org.openygt.production.entity.PatientNotify;
import cn.org.openygt.production.entity.PatientToken;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.entity.StepLog;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskStatusHistory;
import cn.org.openygt.production.mapper.PatientNotifyMapper;
import cn.org.openygt.production.mapper.PatientTokenMapper;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.mapper.StepLogMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.mapper.TaskStatusHistoryMapper;
import cn.org.openygt.production.service.PatientQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientQueryServiceImpl implements PatientQueryService {

    private final PatientTokenMapper patientTokenMapper;
    private final PatientNotifyMapper patientNotifyMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final TaskMapper taskMapper;
    private final StepLogMapper stepLogMapper;
    private final TaskStatusHistoryMapper historyMapper;

    @Override
    public PatientToken queryByCode(String code) {
        // 1. 先按取药码/查询令牌查
        PatientToken result = patientTokenMapper.selectOne(
                new LambdaQueryWrapper<PatientToken>().eq(PatientToken::getToken, code));
        if (result != null) {
            return result;
        }
        // 2. 再按处方号反查 PatientToken
        Prescription prescription = prescriptionMapper.selectOne(
                new LambdaQueryWrapper<Prescription>().eq(Prescription::getPrescriptionNumber, code).last("LIMIT 1"));
        if (prescription != null) {
            return patientTokenMapper.selectOne(
                    new LambdaQueryWrapper<PatientToken>().eq(PatientToken::getPrescriptionId, prescription.getId()));
        }
        // 3. 最后按药袋条码反查（通过 ShelfRecord 找 prescriptionId）
        return null;
    }

    @Override
    public List<Prescription> queryByPhone(String phone) {
        return prescriptionMapper.selectList(
                new LambdaQueryWrapper<Prescription>().eq(Prescription::getPatientPhone, phone)
                        .orderByDesc(Prescription::getCreatedAt));
    }

    @Override
    @Transactional
    public PatientProgressDTO getProgress(String token) {
        PatientToken patientToken = queryByCode(token);
        if (patientToken == null) {
            throw new IllegalArgumentException("查询码无效");
        }

        patientToken.setAccessCount((patientToken.getAccessCount() != null ? patientToken.getAccessCount() : 0) + 1);
        patientTokenMapper.updateById(patientToken);

        Prescription prescription = prescriptionMapper.selectById(patientToken.getPrescriptionId());
        if (prescription == null) {
            throw new IllegalArgumentException("处方不存在");
        }

        Task task = taskMapper.selectOne(
                new LambdaQueryWrapper<Task>().eq(Task::getPrescriptionId, prescription.getId())
                        .orderByDesc(Task::getCreatedAt).last("LIMIT 1"));

        PatientProgressDTO dto = new PatientProgressDTO();
        dto.setPrescriptionId(prescription.getId());
        dto.setPatientName(prescription.getPatientName());
        dto.setPatientPhone(prescription.getPatientPhone());
        dto.setCurrentStatus(task != null ? task.getStatus() : "未知");
        dto.setProgressPercent(calculateProgressPercent(task));
        dto.setEstimatedFinishTime(task != null ? task.getCompleteTime() : null);

        List<PatientProgressDTO.StepItem> steps = new ArrayList<>();
        if (task != null) {
            List<StepLog> logs = stepLogMapper.selectList(
                    new LambdaQueryWrapper<StepLog>().eq(StepLog::getTaskId, task.getId())
                            .orderByAsc(StepLog::getStartedAt));
            for (StepLog log : logs) {
                PatientProgressDTO.StepItem item = new PatientProgressDTO.StepItem();
                item.setStepName(log.getStepType());
                item.setStatus(log.getEndedAt() != null ? "已完成" : "进行中");
                item.setStartTime(log.getStartedAt());
                item.setEndTime(log.getEndedAt());
                steps.add(item);
            }
        }
        dto.setSteps(steps);
        return dto;
    }

    @Override
    public PrescriptionTraceDTO getTrace(Long prescriptionId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new IllegalArgumentException("处方不存在");
        }

        PrescriptionTraceDTO dto = new PrescriptionTraceDTO();
        dto.setPrescriptionId(prescription.getId());
        dto.setPrescriptionNo(prescription.getPrescriptionNumber());
        dto.setPatientName(prescription.getPatientName());

        List<PrescriptionTraceDTO.TraceItem> traces = new ArrayList<>();
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getPrescriptionId, prescriptionId));
        for (Task task : tasks) {
            List<TaskStatusHistory> histories = historyMapper.selectList(
                    new LambdaQueryWrapper<TaskStatusHistory>().eq(TaskStatusHistory::getTaskId, task.getId())
                            .orderByAsc(TaskStatusHistory::getOperateTime));
            for (TaskStatusHistory h : histories) {
                PrescriptionTraceDTO.TraceItem item = new PrescriptionTraceDTO.TraceItem();
                item.setStage(h.getToStatus());
                item.setOperatorName(h.getOperatorId());
                item.setOperateTime(h.getOperateTime());
                item.setRemark(h.getRemark());
                traces.add(item);
            }
        }
        traces.sort(Comparator.comparing(PrescriptionTraceDTO.TraceItem::getOperateTime, Comparator.nullsLast(Comparator.naturalOrder())));
        dto.setTraces(traces);
        return dto;
    }

    private Integer calculateProgressPercent(Task task) {
        if (task == null) return 0;
        if (cn.org.openygt.common.enums.TaskStatus.COMPLETED.getLabel().equals(task.getStatus()) || cn.org.openygt.common.enums.TaskStatus.PARTIAL_COMPLETED.getLabel().equals(task.getStatus())) return 100;
        String status = task.getStatus();
        if (cn.org.openygt.common.enums.TaskStatus.WAIT_SOAK.getLabel().equals(status)) return 0;
        if (cn.org.openygt.common.enums.TaskStatus.SOAKING.getLabel().equals(status)) return 10;
        if (cn.org.openygt.common.enums.TaskStatus.WAIT_DECOCT.getLabel().equals(status)) return 20;
        if (cn.org.openygt.common.enums.TaskStatus.DECOCTING.getLabel().equals(status)) return 40;
        if (cn.org.openygt.common.enums.TaskStatus.WAIT_POUR.getLabel().equals(status)) return 50;
        if (cn.org.openygt.common.enums.TaskStatus.POURING.getLabel().equals(status)) return 60;
        if (cn.org.openygt.common.enums.TaskStatus.WAIT_WRAP.getLabel().equals(status)) return 70;
        if (cn.org.openygt.common.enums.TaskStatus.WRAPPING.getLabel().equals(status)) return 80;
        if (cn.org.openygt.common.enums.TaskStatus.WAIT_LABEL.getLabel().equals(status)) return 90;
        if (cn.org.openygt.common.enums.TaskStatus.WAIT_QC.getLabel().equals(status)) return 95;
        if (cn.org.openygt.common.enums.TaskStatus.WAIT_HANDOVER.getLabel().equals(status)) return 98;
        return 0;
    }
}
