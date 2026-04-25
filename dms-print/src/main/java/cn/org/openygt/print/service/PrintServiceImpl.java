package cn.org.openygt.print.service;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.service.PrintService;
import cn.org.openygt.print.entity.PrintRecord;
import cn.org.openygt.print.entity.PrintTask;
import cn.org.openygt.print.mapper.PrintRecordMapper;
import cn.org.openygt.print.mapper.PrintTaskMapper;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskStatusHistory;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.mapper.TaskStatusHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrintServiceImpl implements PrintService {

    private final TaskMapper taskMapper;
    private final TaskStatusHistoryMapper historyMapper;
    private final PrintTaskMapper printTaskMapper;
    private final PrintRecordMapper printRecordMapper;
    private final EquipmentService equipmentService;

    @Override
    @Transactional
    public void submitPrintTask(Long taskId, String deviceCode, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        if (!"待贴标".equals(task.getStatus())) {
            throw new IllegalStateException("任务不在待贴标状态");
        }

        Object deviceObj = equipmentService.getOrCreateDevice(deviceCode, 4);
        if (deviceObj == null) {
            throw new IllegalStateException("打印机不存在或创建失败");
        }

        Long deviceId = equipmentService.getDeviceId(deviceCode);
        String deviceStatus = equipmentService.getDeviceStatus(deviceId);
        if (!"IDLE".equalsIgnoreCase(deviceStatus) && !"idle".equalsIgnoreCase(deviceStatus)) {
            throw new IllegalStateException("打印机不是空闲状态");
        }

        doPrint(task, deviceId, deviceCode, operatorId);
    }

    @Override
    @Transactional
    public void retryPrint(Long taskId, String deviceCode, String operatorId) {
        Task task = getTaskOrThrow(taskId);
        if (!"FAILED".equals(task.getPrintStatus()) && !"PENDING".equals(task.getPrintStatus()) && !"PRINTED".equals(task.getPrintStatus())) {
            throw new IllegalStateException("当前任务不可重试打印");
        }

        task.setPrintStatus("PENDING");
        task.setPrintDeviceId(null);
        task.setPrintTime(null);
        taskMapper.updateById(task);

        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getTaskId, taskId)
               .orderByDesc(PrintTask::getCreatedAt)
               .last("LIMIT 1");
        PrintTask lastPrintTask = printTaskMapper.selectOne(wrapper);
        if (lastPrintTask != null) {
            lastPrintTask.setRetryCount((lastPrintTask.getRetryCount() != null ? lastPrintTask.getRetryCount() : 0) + 1);
            printTaskMapper.updateById(lastPrintTask);
        }

        submitPrintTask(taskId, deviceCode, operatorId);
    }

    @Override
    public Object getPrintQueue() {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getStatus, "PENDING")
               .or().eq(PrintTask::getStatus, "FAILED");
        wrapper.orderByDesc(PrintTask::getCreatedAt);
        return printTaskMapper.selectList(wrapper);
    }

    private void doPrint(Task task, Long deviceId, String deviceCode, String operatorId) {
        // 创建打印任务记录
        PrintTask printTask = new PrintTask();
        printTask.setTaskId(task.getId());
        printTask.setDeviceCode(deviceCode);
        printTask.setOperatorId(operatorId);
        printTask.setStatus("PRINTING");
        printTask.setCopies(task.getPrintCopies() != null ? task.getPrintCopies() : 1);
        printTask.setRetryCount(0);
        printTaskMapper.insert(printTask);

        // 占用打印机
        equipmentService.updateDeviceStatus(deviceId, "RUNNING");

        // 更新生产任务打印状态
        task.setPrintDeviceId(deviceId);
        task.setPrintStatus("PRINTING");
        taskMapper.updateById(task);

        // 执行打印（同步模拟）
        task.setPrintStatus("PRINTED");
        task.setPrintTime(new Date());
        taskMapper.updateById(task);

        // 释放打印机
        equipmentService.releaseDevice(deviceId);

        // 更新打印任务状态
        printTask.setStatus("COMPLETED");
        printTaskMapper.updateById(printTask);

        // 记录打印结果
        PrintRecord record = new PrintRecord();
        record.setPrintTaskId(printTask.getId());
        record.setResult("SUCCESS");
        printRecordMapper.insert(record);

        // 记录历史
        recordHistory(task.getId(), null, "PRINTED", operatorId, "打印标签成功，打印机: " + deviceCode);

        log.info("打印标签成功: taskId={}, deviceCode={}, operatorId={}", task.getId(), deviceCode, operatorId);
    }

    private Task getTaskOrThrow(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        return task;
    }

    private void recordHistory(Long taskId, String fromStatus, String toStatus, String operatorId, String remark) {
        TaskStatusHistory history = new TaskStatusHistory();
        history.setTaskId(taskId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorId(operatorId);
        history.setOperateTime(new Date());
        history.setRemark(remark);
        historyMapper.insert(history);
    }
}
