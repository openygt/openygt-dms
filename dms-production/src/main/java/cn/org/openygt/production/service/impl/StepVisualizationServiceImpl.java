package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.StepDetailDTO;
import cn.org.openygt.production.dto.StepInfoDTO;
import cn.org.openygt.production.entity.StepLog;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.WorkRecord;
import cn.org.openygt.production.mapper.StepLogMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.mapper.WorkRecordMapper;
import cn.org.openygt.system.mapper.SysUserMapper;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.production.service.StepVisualizationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StepVisualizationServiceImpl implements StepVisualizationService {

    private final TaskMapper taskMapper;
    private final StepLogMapper stepLogMapper;
    private final WorkRecordMapper workRecordMapper;
    private final SysUserMapper sysUserMapper;

    /** 展示步骤定义（前端展示用，顺序固定） */
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

    /**
     * 展示码 → 写库码映射。
     * 状态机实际写入 StepLog / WorkRecord 的 stepType / action 与展示码不完全一致，
     * 查询时需先做映射，否则步骤明细和工时记录会系统性为空。
     */
    private static final Map<String, String> DISPLAY_TO_LOG_CODE = new HashMap<>();
    static {
        DISPLAY_TO_LOG_CODE.put("SOAK", "SOAK");
        DISPLAY_TO_LOG_CODE.put("FIRST_DECOCTION", "DECOCT");
        DISPLAY_TO_LOG_CODE.put("SECOND_DECOCTION", "DECOCT");
        DISPLAY_TO_LOG_CODE.put("POUR", "POUR");
        DISPLAY_TO_LOG_CODE.put("WRAP", "WRAP");
        DISPLAY_TO_LOG_CODE.put("QC", "INSPECT");
        DISPLAY_TO_LOG_CODE.put("DELIVER", "HANDOVER");
        // RECEIVE / ADJUST 无写库记录，保持虚拟
    }

    /** 将展示码转换为写库码；无映射时返回自身（兼容直接以写库码查询的场景） */
    private String toLogCode(String displayCode) {
        return DISPLAY_TO_LOG_CODE.getOrDefault(displayCode, displayCode);
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

        // 批量查询操作人姓名
        java.util.Set<String> opIds = stepLogs.stream()
                .map(StepLog::getOperatorId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Map<String, String> nameMap = new java.util.HashMap<>();
        if (!opIds.isEmpty()) {
            java.util.List<SysUser> users = sysUserMapper.selectBatchIds(opIds.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList()));
            nameMap = users.stream().collect(java.util.stream.Collectors.toMap(u -> String.valueOf(u.getId()), SysUser::getRealName, (a, b) -> a));
        }
        for (Map.Entry<String, String> entry : STEP_DEFINITIONS.entrySet()) {
            String stepCode = entry.getKey();
            String stepName = entry.getValue();
            String logCode = toLogCode(stepCode);
            List<StepLog> logs = logMap.getOrDefault(logCode, java.util.Collections.emptyList());

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
                dto.setOperatorName(nameMap.getOrDefault(latest.getOperatorId(), latest.getOperatorId()));
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

        String logCode = toLogCode(stepCode);

        LambdaQueryWrapper<StepLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StepLog::getTaskId, taskId)
                .eq(StepLog::getStepType, logCode)
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
                .eq(WorkRecord::getAction, logCode)
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
