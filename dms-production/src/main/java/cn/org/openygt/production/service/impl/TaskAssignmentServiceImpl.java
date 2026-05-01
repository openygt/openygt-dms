package cn.org.openygt.production.service.impl;

import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.production.dto.DeviceLoadDTO;
import cn.org.openygt.production.dto.EmployeeLoadDTO;
import cn.org.openygt.production.dto.GanttItemDTO;
import cn.org.openygt.production.entity.EmployeeSkill;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskAssignment;
import cn.org.openygt.production.mapper.EmployeeSkillMapper;
import cn.org.openygt.production.mapper.TaskAssignmentMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.service.TaskAssignmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAssignmentServiceImpl implements TaskAssignmentService {

    private final TaskAssignmentMapper assignmentMapper;
    private final EmployeeSkillMapper employeeSkillMapper;
    private final TaskMapper taskMapper;
    private final EquipmentService equipmentService;

    @Override
    @Transactional
    public TaskAssignment autoAssign(Long taskId, String strategy) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(taskId);
        assignment.setPrescriptionId(task.getPrescriptionId());
        assignment.setStatus(0);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());

        String selectedStrategy = strategy != null ? strategy.toUpperCase() : "LOAD_BALANCE";
        switch (selectedStrategy) {
            case "SKILL":
                assignBySkill(task, assignment);
                break;
            case "URGENCY":
                assignByUrgency(task, assignment);
                break;
            case "SIMILARITY":
                assignBySimilarity(task, assignment);
                break;
            case "LOAD_BALANCE":
            default:
                assignByLoadBalance(task, assignment);
                break;
        }

        assignment.setAssignType(1);
        assignment.setAssignReason("自动分配-策略:" + selectedStrategy);
        assignmentMapper.insert(assignment);
        return assignment;
    }

    @Override
    @Transactional
    public TaskAssignment manualAssign(Long taskId, Long deviceId, Long employeeId, String reason) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(taskId);
        assignment.setPrescriptionId(task.getPrescriptionId());
        assignment.setDeviceId(deviceId);
        assignment.setEmployeeId(employeeId);
        assignment.setAssignType(2);
        assignment.setAssignReason(reason);
        assignment.setStatus(0);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentMapper.insert(assignment);
        return assignment;
    }

    @Override
    @Transactional
    public TaskAssignment reassign(Long assignmentId, Long newDeviceId, Long newEmployeeId, String reason) {
        TaskAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("分配记录不存在");
        }
        assignment.setDeviceId(newDeviceId);
        assignment.setEmployeeId(newEmployeeId);
        assignment.setAssignReason((assignment.getAssignReason() != null ? assignment.getAssignReason() + "; " : "") + "重新分配: " + reason);
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentMapper.updateById(assignment);
        return assignment;
    }

    @Override
    public List<GanttItemDTO> getSchedule(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<TaskAssignment> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(TaskAssignment::getScheduledStartTime, startTime).or().ge(TaskAssignment::getActualStartTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(TaskAssignment::getScheduledEndTime, endTime).or().le(TaskAssignment::getActualEndTime, endTime);
        }
        List<TaskAssignment> list = assignmentMapper.selectList(wrapper);
        List<GanttItemDTO> result = new ArrayList<>();
        for (TaskAssignment a : list) {
            GanttItemDTO dto = new GanttItemDTO();
            dto.setAssignmentId(a.getId());
            dto.setTaskId(a.getTaskId());
            dto.setTaskName("任务-" + a.getTaskId());
            dto.setDeviceId(a.getDeviceId());
            dto.setEmployeeId(a.getEmployeeId());
            if (a.getDeviceId() != null) {
                try {
                    EqDeviceDTO dev = equipmentService.getDeviceById(a.getDeviceId());
                    dto.setDeviceName(dev != null ? dev.getName() : null);
                } catch (Exception ignored) {
                }
            }
            dto.setStartTime(a.getActualStartTime() != null ? a.getActualStartTime() : a.getScheduledStartTime());
            dto.setEndTime(a.getActualEndTime() != null ? a.getActualEndTime() : a.getScheduledEndTime());
            dto.setStatus(String.valueOf(a.getStatus()));
            dto.setProgress(calculateProgress(a));
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<EmployeeLoadDTO> getEmployeeLoad() {
        List<TaskAssignment> all = assignmentMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, List<TaskAssignment>> grouped = all.stream().filter(a -> a.getEmployeeId() != null)
                .collect(Collectors.groupingBy(TaskAssignment::getEmployeeId));
        List<EmployeeLoadDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<TaskAssignment>> entry : grouped.entrySet()) {
            EmployeeLoadDTO dto = new EmployeeLoadDTO();
            dto.setEmployeeId(entry.getKey());
            dto.setAssignedCount(entry.getValue().size());
            dto.setCompletedCount((int) entry.getValue().stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count());
            dto.setPendingCount((int) entry.getValue().stream().filter(a -> a.getStatus() == null || a.getStatus() == 0).count());
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<DeviceLoadDTO> getDeviceLoad() {
        List<TaskAssignment> all = assignmentMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, List<TaskAssignment>> grouped = all.stream().filter(a -> a.getDeviceId() != null)
                .collect(Collectors.groupingBy(TaskAssignment::getDeviceId));
        List<DeviceLoadDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<TaskAssignment>> entry : grouped.entrySet()) {
            DeviceLoadDTO dto = new DeviceLoadDTO();
            dto.setDeviceId(entry.getKey());
            try {
                EqDeviceDTO dev = equipmentService.getDeviceById(entry.getKey());
                if (dev != null) {
                    dto.setDeviceCode(dev.getDeviceCode());
                    dto.setDeviceName(dev.getName());
                    dto.setIdleCount("idle".equalsIgnoreCase(dev.getStatus()) ? 1 : 0);
                    dto.setRunningCount("running".equalsIgnoreCase(dev.getStatus()) ? 1 : 0);
                }
            } catch (Exception ignored) {
            }
            dto.setAssignedCount(entry.getValue().size());
            result.add(dto);
        }
        return result;
    }

    private void assignByLoadBalance(Task task, TaskAssignment assignment) {
        List<TaskAssignment> active = assignmentMapper.selectList(
                new LambdaQueryWrapper<TaskAssignment>().eq(TaskAssignment::getStatus, 0));
        Map<Long, Long> countMap = active.stream().filter(a -> a.getEmployeeId() != null)
                .collect(Collectors.groupingBy(TaskAssignment::getEmployeeId, Collectors.counting()));
        Long minEmployeeId = countMap.entrySet().stream().min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        assignment.setEmployeeId(minEmployeeId);
        assignment.setDeviceId(task.getDecoctDeviceId());
    }

    private void assignBySkill(Task task, TaskAssignment assignment) {
        String skillHint = task.getCurrentStep() != null ? task.getCurrentStep() : "DECOCT";
        List<EmployeeSkill> skills = employeeSkillMapper.selectList(
                new LambdaQueryWrapper<EmployeeSkill>().eq(EmployeeSkill::getSkillCode, skillHint));
        Long employeeId = skills.stream().max(Comparator.comparingInt(EmployeeSkill::getProficiencyLevel))
                .map(EmployeeSkill::getEmployeeId).orElse(null);
        assignment.setEmployeeId(employeeId);
        assignment.setDeviceId(task.getDecoctDeviceId());
    }

    private void assignByUrgency(Task task, TaskAssignment assignment) {
        assignByLoadBalance(task, assignment);
        assignment.setAssignReason("紧急任务优先分配");
    }

    private void assignBySimilarity(Task task, TaskAssignment assignment) {
        if (task.getPrescriptionId() != null) {
            List<TaskAssignment> similar = assignmentMapper.selectList(
                    new LambdaQueryWrapper<TaskAssignment>().eq(TaskAssignment::getPrescriptionId, task.getPrescriptionId())
                            .orderByDesc(TaskAssignment::getCreatedAt).last("LIMIT 1"));
            if (!similar.isEmpty()) {
                assignment.setEmployeeId(similar.get(0).getEmployeeId());
                assignment.setDeviceId(similar.get(0).getDeviceId());
                return;
            }
        }
        assignByLoadBalance(task, assignment);
    }

    private Integer calculateProgress(TaskAssignment a) {
        if (a.getStatus() != null && a.getStatus() == 1) return 100;
        if (a.getActualStartTime() != null && a.getScheduledEndTime() != null) {
            long total = java.time.Duration.between(a.getScheduledStartTime() != null ? a.getScheduledStartTime() : a.getActualStartTime(), a.getScheduledEndTime()).toMinutes();
            long past = java.time.Duration.between(a.getActualStartTime(), LocalDateTime.now()).toMinutes();
            if (total <= 0) return 0;
            return (int) Math.min(100, Math.max(0, past * 100 / total));
        }
        return 0;
    }
}
