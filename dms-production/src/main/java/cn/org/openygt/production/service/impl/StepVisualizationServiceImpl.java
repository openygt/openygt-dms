package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.StepDetailDTO;
import cn.org.openygt.production.dto.StepInfoDTO;
import cn.org.openygt.production.entity.StepLog;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.WorkRecord;
import cn.org.openygt.production.mapper.StepLogMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.mapper.WorkRecordMapper;
import cn.org.openygt.production.service.StepVisualizationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StepVisualizationServiceImpl implements StepVisualizationService {

    private final TaskMapper taskMapper;
    private final StepLogMapper stepLogMapper;
    private final WorkRecordMapper workRecordMapper;

    private static final Map<String, String> STEP_DEFINITIONS = new LinkedHashMap<>();
    static {
        STEP_DEFINITIONS.put("RECEIVE", "接收");
        STEP_DEFINITIONS.put("ADJUST", "调配");
        STEP_DEFINITIONS.put("SOAK", "泡药");
        STEP_DEFINITIONS.put("FIRST_DECOCTION", "一煎");
        STEP_DEFINITIONS.put("SECOND_DECOCTION", "二煎");
        STEP_DEFINITIONS.put("POUR", "出液");
        STEP_DEFINITIONS.put("WRAP", "包装");
        STEP_DEFINITIONS.put("QC", "质检");
        STEP_DEFINITIONS.put("DELIVER", "交付");
    }

    @Override
    public List<StepInfoDTO> getTaskSteps(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        LambdaQueryWrapper<StepLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StepLog::getTaskId, taskId).orderByAsc(StepLog::getStartedAt);
        List<StepLog> stepLogs = stepLogMapper.selectList(wrapper);

        Map<String, List<StepLog>> logMap = stepLogs.stream()
                .collect(Collectors.groupingBy(StepLog::getStepType));

        List<StepInfoDTO> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : STEP_DEFINITIONS.entrySet()) {
            String stepCode = entry.getKey();
            String stepName = entry.getValue();
            List<StepLog> logs = logMap.getOrDefault(stepCode, java.util.Collections.emptyList());

            StepInfoDTO dto = new StepInfoDTO();
            dto.setStepCode(stepCode);
            dto.setStepName(stepName);

            if (logs.isEmpty()) {
                dto.setStatus("PENDING");
            } else {
                StepLog latest = logs.get(logs.size() - 1);
                dto.setStatus(latest.getEndedAt() != null ? "COMPLETED" : "PROCESSING");
                dto.setStartTime(latest.getStartedAt());
                dto.setEndTime(latest.getEndedAt());
                dto.setOperatorId(latest.getOperatorId());
                dto.setResult(latest.getResult());
                if (latest.getStartedAt() != null && latest.getEndedAt() != null) {
                    dto.setDurationMinutes((int) ChronoUnit.MINUTES.between(latest.getStartedAt(), latest.getEndedAt()));
                }
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public StepDetailDTO getStepDetail(Long taskId, String stepCode) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        LambdaQueryWrapper<StepLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StepLog::getTaskId, taskId)
                .eq(StepLog::getStepType, stepCode)
                .orderByDesc(StepLog::getStartedAt)
                .last("LIMIT 1");
        StepLog stepLog = stepLogMapper.selectOne(wrapper);

        if (stepLog == null) {
            StepDetailDTO empty = new StepDetailDTO();
            empty.setStepCode(stepCode);
            empty.setStepName(STEP_DEFINITIONS.getOrDefault(stepCode, stepCode));
            empty.setStatus("PENDING");
            return empty;
        }

        StepDetailDTO dto = new StepDetailDTO();
        dto.setStepCode(stepCode);
        dto.setStepName(STEP_DEFINITIONS.getOrDefault(stepCode, stepCode));
        dto.setStatus(stepLog.getEndedAt() != null ? "COMPLETED" : "PROCESSING");
        dto.setStartTime(stepLog.getStartedAt());
        dto.setEndTime(stepLog.getEndedAt());
        dto.setOperatorId(stepLog.getOperatorId());
        dto.setPauseDuration(stepLog.getPauseDuration());
        dto.setPauseReason(stepLog.getPauseReason());
        dto.setDelayMinutes(stepLog.getDelayMinutes());
        dto.setDelayReason(stepLog.getDelayReason());
        dto.setResult(stepLog.getResult());
        dto.setAbortReason(stepLog.getAbortReason());
        dto.setWasteAmount(stepLog.getWasteAmount());
        dto.setWasteUnit(stepLog.getWasteUnit());

        if (stepLog.getStartedAt() != null && stepLog.getEndedAt() != null) {
            dto.setDurationMinutes((int) ChronoUnit.MINUTES.between(stepLog.getStartedAt(), stepLog.getEndedAt()));
        }

        LambdaQueryWrapper<WorkRecord> workWrapper = new LambdaQueryWrapper<>();
        workWrapper.eq(WorkRecord::getTaskId, taskId)
                .eq(WorkRecord::getAction, stepCode)
                .orderByDesc(WorkRecord::getCreatedAt);
        List<WorkRecord> workRecords = workRecordMapper.selectList(workWrapper);
        dto.setWorkRecords(workRecords.stream().map(w -> {
            StepDetailDTO.WorkRecordDTO wd = new StepDetailDTO.WorkRecordDTO();
            wd.setOperatorId(w.getOperatorId());
            wd.setOperatorName(w.getOperatorName());
            wd.setAction(w.getAction());
            wd.setWorkTime(w.getWorkTime());
            wd.setCreatedAt(w.getCreatedAt());
            return wd;
        }).collect(Collectors.toList()));

        return dto;
    }
}
