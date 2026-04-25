package cn.org.openygt.print.service;

import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.dto.PrintTaskDTO;
import cn.org.openygt.common.enums.PrintTaskStatus;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.service.PrintService;
import cn.org.openygt.print.entity.PrintRecord;
import cn.org.openygt.print.entity.PrintTask;
import cn.org.openygt.print.mapper.PrintRecordMapper;
import cn.org.openygt.print.mapper.PrintTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrintServiceImpl implements PrintService {

    private final PrintTaskMapper printTaskMapper;
    private final PrintRecordMapper printRecordMapper;
    private final EquipmentService equipmentService;

    @Override
    @Transactional
    public void submitPrintTask(Long taskId, String deviceCode, String operatorId) {
        EqDeviceDTO device = equipmentService.getOrCreateDevice(deviceCode, 4);
        if (device == null) {
            throw new IllegalStateException("打印机不存在或创建失败");
        }

        Long deviceId = device.getId();
        String deviceStatus = equipmentService.getDeviceStatus(deviceId);
        if (!"IDLE".equalsIgnoreCase(deviceStatus) && !"idle".equalsIgnoreCase(deviceStatus)) {
            throw new IllegalStateException("打印机不是空闲状态");
        }

        doPrint(taskId, deviceId, deviceCode, operatorId);
    }

    @Override
    @Transactional
    public void retryPrint(Long taskId, String deviceCode, String operatorId) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getTaskId, taskId)
               .orderByDesc(PrintTask::getCreatedAt)
               .last("LIMIT 1");
        PrintTask lastPrintTask = printTaskMapper.selectOne(wrapper);
        if (lastPrintTask != null) {
            lastPrintTask.setRetryCount((lastPrintTask.getRetryCount() != null ? lastPrintTask.getRetryCount() : 0) + 1);
            lastPrintTask.setStatus(PrintTaskStatus.PENDING.name());
            printTaskMapper.updateById(lastPrintTask);
        }

        submitPrintTask(taskId, deviceCode, operatorId);
    }

    @Override
    public List<PrintTaskDTO> getPrintQueue() {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getStatus, PrintTaskStatus.PENDING.name())
               .or().eq(PrintTask::getStatus, PrintTaskStatus.FAILED.name());
        wrapper.orderByDesc(PrintTask::getCreatedAt);
        List<PrintTask> tasks = printTaskMapper.selectList(wrapper);
        return tasks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private void doPrint(Long taskId, Long deviceId, String deviceCode, String operatorId) {
        // 创建打印任务记录
        PrintTask printTask = new PrintTask();
        printTask.setTaskId(taskId);
        printTask.setDeviceCode(deviceCode);
        printTask.setOperatorId(operatorId);
        printTask.setStatus(PrintTaskStatus.PENDING.name());
        printTask.setCopies(1);
        printTask.setRetryCount(0);
        printTaskMapper.insert(printTask);

        // 占用打印机
        equipmentService.updateDeviceStatus(deviceId, "RUNNING");

        // 执行打印（同步模拟）
        printTask.setStatus(PrintTaskStatus.COMPLETED.name());
        printTaskMapper.updateById(printTask);

        // 释放打印机
        equipmentService.releaseDevice(deviceId);

        // 记录打印结果
        PrintRecord record = new PrintRecord();
        record.setPrintTaskId(printTask.getId());
        record.setResult("SUCCESS");
        printRecordMapper.insert(record);

        log.info("打印标签成功: taskId={}, deviceCode={}, operatorId={}", taskId, deviceCode, operatorId);
    }

    private PrintTaskDTO toDTO(PrintTask task) {
        PrintTaskDTO dto = new PrintTaskDTO();
        dto.setId(task.getId());
        dto.setTaskId(task.getTaskId());
        dto.setDeviceCode(task.getDeviceCode());
        dto.setOperatorId(task.getOperatorId());
        dto.setStatus(task.getStatus());
        dto.setCopies(task.getCopies());
        dto.setRetryCount(task.getRetryCount());
        dto.setCreatedAt(task.getCreatedAt());
        return dto;
    }
}
