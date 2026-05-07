package cn.org.openygt.production.service.impl;

import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.enums.DeviceDetailStatus;
import cn.org.openygt.production.dto.DeviceLoadDTO;
import cn.org.openygt.production.dto.EmployeeLoadDTO;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.system.mapper.SysUserMapper;
import cn.org.openygt.production.dto.GanttItemDTO;
import cn.org.openygt.production.dto.OccupiedIds;
import cn.org.openygt.production.dto.TaskOptionDTO;
import cn.org.openygt.production.entity.EmployeeSkill;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskAssignment;
import cn.org.openygt.production.mapper.EmployeeSkillMapper;
import cn.org.openygt.production.mapper.HrEmployeeMapper;
import cn.org.openygt.production.mapper.TaskAssignmentMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.service.TaskAssignmentService;
import cn.org.openygt.equipment.enums.DeviceDetailStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAssignmentServiceImpl implements TaskAssignmentService {

    private final TaskAssignmentMapper assignmentMapper;
    private final EmployeeSkillMapper employeeSkillMapper;
    private final TaskMapper taskMapper;
    private final EquipmentService equipmentService;
    private final SysUserMapper sysUserMapper;
    private final HrEmployeeMapper hrEmployeeMapper;

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
        assignment.setStatus(1);
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
    public TaskAssignment manualAssign(Long taskId, Long deviceId, Long employeeId, String reason, LocalDate scheduledDate) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        LocalDate targetDate = scheduledDate != null ? scheduledDate : LocalDate.now();

        // 去重检查：同一任务不能重复分配
        Long existCount = assignmentMapper.selectCount(
                new LambdaQueryWrapper<TaskAssignment>()
                        .eq(TaskAssignment::getTaskId, taskId)
                        .in(TaskAssignment::getStatus, 1, 2));
        if (existCount > 0) {
            throw new IllegalArgumentException("该任务已有待执行或执行中的分配记录，不能重复分配");
        }

        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.plusDays(1).atStartOfDay();

        // 去重检查：同一员工不能重复分配到同一任务（按任务去重，允许多任务）
        // 业务场景：一个煎药工一天处理7-8个任务，需要支持同一天多次记录

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(taskId);
        assignment.setPrescriptionId(task.getPrescriptionId());
        assignment.setDeviceId(deviceId);
        assignment.setEmployeeId(employeeId);
        assignment.setAssignType(2);
        assignment.setAssignReason(reason);
        assignment.setStatus(1);
        assignment.setScheduledStartTime(targetDate.atStartOfDay());
        assignment.setScheduledEndTime(targetDate.atStartOfDay().plusHours(8));
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

        LocalDate targetDate = assignment.getScheduledStartTime() != null
                ? assignment.getScheduledStartTime().toLocalDate()
                : assignment.getCreatedAt() != null ? assignment.getCreatedAt().toLocalDate() : LocalDate.now();
        // 修改记录只需检查任务去重（已在manualAssign中处理），
        // 不再限制同一天员工/设备数量（一个煎药工一天处理多个任务）

        // P0-4: 字段保护，null 时保留原值
        if (newDeviceId != null) {
            assignment.setDeviceId(newDeviceId);
        }
        if (newEmployeeId != null) {
            assignment.setEmployeeId(newEmployeeId);
        }
        assignment.setAssignReason((assignment.getAssignReason() != null ? assignment.getAssignReason() + "; " : "") + "重新分配: " + reason);
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentMapper.updateById(assignment);
        return assignment;
    }

    @Override
    public List<GanttItemDTO> getSchedule(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<TaskAssignment> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.and(w -> w.ge(TaskAssignment::getScheduledStartTime, startTime)
                    .or().ge(TaskAssignment::getActualStartTime, startTime)
                    .or().ge(TaskAssignment::getCreatedAt, startTime));
        }
        if (endTime != null) {
            wrapper.and(w -> w.le(TaskAssignment::getScheduledEndTime, endTime)
                    .or().le(TaskAssignment::getActualEndTime, endTime)
                    .or().le(TaskAssignment::getCreatedAt, endTime));
        }
        List<TaskAssignment> list = assignmentMapper.selectList(wrapper);

        // 收集所有 employeeId 并批量查询姓名
        Set<Long> allEmployeeIds = list.stream()
                .map(TaskAssignment::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userNameMap = queryEmployeeNames(allEmployeeIds);

        List<GanttItemDTO> result = new ArrayList<>();
        for (TaskAssignment a : list) {
            GanttItemDTO dto = new GanttItemDTO();
            dto.setAssignmentId(a.getId());
            dto.setTaskId(a.getTaskId());
            dto.setTaskName("任务-" + a.getTaskId());
            dto.setDeviceId(a.getDeviceId());
            dto.setEmployeeId(a.getEmployeeId());
            dto.setEmployeeName(a.getEmployeeId() != null ? userNameMap.get(a.getEmployeeId()) : null);
            dto.setAssignType(a.getAssignType());
            dto.setCreatedAt(a.getCreatedAt());
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
    public List<EmployeeLoadDTO> getEmployeeLoad(LocalDate date) {
        LambdaQueryWrapper<TaskAssignment> wrapper = new LambdaQueryWrapper<>();
        if (date != null) {
            wrapper.apply("COALESCE(scheduled_start_time, created_at) BETWEEN {0} AND {1}",
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }
        List<TaskAssignment> all = assignmentMapper.selectList(wrapper);
        Map<Long, List<TaskAssignment>> grouped = all.stream().filter(a -> a.getEmployeeId() != null)
                .collect(Collectors.groupingBy(TaskAssignment::getEmployeeId));

        // 查询员工姓名
        Map<Long, String> userNameMap = queryEmployeeNames(grouped.keySet());

        List<EmployeeLoadDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<TaskAssignment>> entry : grouped.entrySet()) {
            EmployeeLoadDTO dto = new EmployeeLoadDTO();
            dto.setEmployeeId(entry.getKey());
            dto.setEmployeeName(userNameMap.getOrDefault(entry.getKey(), "员工-" + entry.getKey()));
            dto.setAssignedCount(entry.getValue().size());
            dto.setCompletedCount((int) entry.getValue().stream().filter(a -> a.getStatus() != null && a.getStatus() == 3).count());
            dto.setPendingCount((int) entry.getValue().stream().filter(a -> a.getStatus() == null || a.getStatus() == 0 || a.getStatus() == 1).count());
            result.add(dto);
        }
        return result;
    }

    /**
     * 根据员工ID集合查询 sys_user 表，返回 id → realName 映射
     */
    private Map<Long, String> queryEmployeeNames(Set<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(employeeIds);
        return users.stream()
                .filter(u -> u.getRealName() != null)
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
    }

    @Override
    public List<DeviceLoadDTO> getDeviceLoad(LocalDate date) {
        LambdaQueryWrapper<TaskAssignment> wrapper = new LambdaQueryWrapper<>();
        if (date != null) {
            wrapper.apply("COALESCE(scheduled_start_time, created_at) BETWEEN {0} AND {1}",
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }
        List<TaskAssignment> all = assignmentMapper.selectList(wrapper);
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
                    // 使用 DeviceDetailStatus.isRunning() 判断运行状态
                    boolean isRunning = DeviceDetailStatus.isRunning(dev.getStatus());
                    dto.setIdleCount(!isRunning ? 1 : 0);
                    dto.setRunningCount(isRunning ? 1 : 0);
                }
            } catch (Exception ignored) {
            }
            dto.setAssignedCount(entry.getValue().size());
            result.add(dto);
        }
        return result;
    }

    @Override
    public OccupiedIds getOccupiedIds(LocalDate date) {
        OccupiedIds occupied = new OccupiedIds();

        // 任务：只要有待执行/执行中的分配，无论日期都视为已占用
        LambdaQueryWrapper<TaskAssignment> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(TaskAssignment::getStatus, 1, 2);
        List<TaskAssignment> allActive = assignmentMapper.selectList(taskWrapper);
        occupied.setTaskIds(allActive.stream().map(TaskAssignment::getTaskId).filter(Objects::nonNull).distinct().collect(Collectors.toList()));

        // 员工/设备：按日期筛选，同一天已分配的视为已占用
        LambdaQueryWrapper<TaskAssignment> dateWrapper = new LambdaQueryWrapper<>();
        if (date != null) {
            dateWrapper.apply("COALESCE(scheduled_start_time, created_at) BETWEEN {0} AND {1}",
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }
        dateWrapper.in(TaskAssignment::getStatus, 1, 2);
        List<TaskAssignment> dateFiltered = assignmentMapper.selectList(dateWrapper);
        occupied.setEmployeeIds(dateFiltered.stream().map(TaskAssignment::getEmployeeId).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        occupied.setDeviceIds(dateFiltered.stream().map(TaskAssignment::getDeviceId).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        return occupied;
    }

    @Override
    public List<TaskOptionDTO> getAvailableTasks(LocalDate date) {
        List<Task> all = taskMapper.selectList(new LambdaQueryWrapper<>());
        List<Long> assignedTaskIds = assignmentMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .filter(a -> a.getStatus() == null || a.getStatus() == 0)
                .map(TaskAssignment::getTaskId).collect(Collectors.toList());
        return all.stream()
                .filter(t -> !assignedTaskIds.contains(t.getId()))
                .map(t -> new TaskOptionDTO(t.getId(), "任务-" + t.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EmployeeLoadDTO> getAvailableEmployees(LocalDate date) {
        return getEmployeeLoad(date).stream()
                .filter(e -> e.getAssignedCount() < 10)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeviceLoadDTO> getAvailableDevices(LocalDate date) {
        return getDeviceLoad(date).stream()
                .filter(d -> d.getRunningCount() == null || d.getRunningCount() < 2)
                .collect(Collectors.toList());
    }

    private void assignByLoadBalance(Task task, TaskAssignment assignment) {
        List<TaskAssignment> active = assignmentMapper.selectList(
                new LambdaQueryWrapper<TaskAssignment>().eq(TaskAssignment::getStatus, 1));
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
        if (a.getStatus() != null && a.getStatus() == 3) return 100;
        if (a.getActualStartTime() != null && a.getScheduledEndTime() != null) {
            long total = java.time.Duration.between(a.getScheduledStartTime() != null ? a.getScheduledStartTime() : a.getActualStartTime(), a.getScheduledEndTime()).toMinutes();
            long past = java.time.Duration.between(a.getActualStartTime(), LocalDateTime.now()).toMinutes();
            if (total <= 0) return 0;
            return (int) Math.min(100, Math.max(0, past * 100 / total));
        }
        return 0;
    }
}
