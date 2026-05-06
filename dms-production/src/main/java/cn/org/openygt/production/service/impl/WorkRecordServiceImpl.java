package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.EmployeeWorkDailyDTO;
import cn.org.openygt.production.entity.WorkRecord;
import cn.org.openygt.production.mapper.WorkRecordMapper;
import cn.org.openygt.production.service.WorkRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkRecordServiceImpl implements WorkRecordService {

    private final WorkRecordMapper workRecordMapper;

    private static final Map<String, String> ACTION_LABEL = new HashMap<>();
    static {
        ACTION_LABEL.put("SOAK", "泡药");
        ACTION_LABEL.put("DECOCT", "煎药");
        ACTION_LABEL.put("WRAP", "包装");
        ACTION_LABEL.put("POUR", "出液");
        ACTION_LABEL.put("LABEL", "贴标");
        ACTION_LABEL.put("INSPECT", "质检");
        ACTION_LABEL.put("HANDOVER", "交接");
    }

    @Override
    public List<EmployeeWorkDailyDTO> getDailyReport(LocalDate date) {
        LambdaQueryWrapper<WorkRecord> wrapper = new LambdaQueryWrapper<>();
        if (date != null) {
            wrapper.between(WorkRecord::getCreatedAt, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }
        List<WorkRecord> all = workRecordMapper.selectList(wrapper);

        Map<String, List<WorkRecord>> byOperator = all.stream()
            .filter(r -> r.getOperatorId() != null)
            .collect(Collectors.groupingBy(WorkRecord::getOperatorId));

        List<EmployeeWorkDailyDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<WorkRecord>> entry : byOperator.entrySet()) {
            EmployeeWorkDailyDTO dto = new EmployeeWorkDailyDTO();
            dto.setOperatorId(entry.getKey());
            List<WorkRecord> records = entry.getValue();
            dto.setOperatorName(
                records.stream().map(WorkRecord::getOperatorName).filter(Objects::nonNull).findFirst().orElse(entry.getKey())
            );

            Map<String, List<WorkRecord>> byAction = records.stream()
                .collect(Collectors.groupingBy(r -> r.getAction() != null ? r.getAction() : "OTHER"));

            List<EmployeeWorkDailyDTO.ActionSummary> actions = new ArrayList<>();
            int total = 0;
            for (Map.Entry<String, List<WorkRecord>> actEntry : byAction.entrySet()) {
                EmployeeWorkDailyDTO.ActionSummary summary = new EmployeeWorkDailyDTO.ActionSummary();
                summary.setAction(actEntry.getKey());
                summary.setActionLabel(ACTION_LABEL.getOrDefault(actEntry.getKey(), actEntry.getKey()));
                summary.setCount(actEntry.getValue().size());
                int mins = actEntry.getValue().stream().mapToInt(r -> r.getWorkTime() != null ? r.getWorkTime() : 0).sum();
                summary.setTotalMinutes(mins);
                total += mins;
                actions.add(summary);
            }
            actions.sort(Comparator.comparingInt(EmployeeWorkDailyDTO.ActionSummary::getTotalMinutes).reversed());
            dto.setActions(actions);
            dto.setTotalMinutes(total);
            result.add(dto);
        }

        result.sort(Comparator.comparingInt(EmployeeWorkDailyDTO::getTotalMinutes).reversed());
        return result;
    }
}
