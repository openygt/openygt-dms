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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrintServiceImpl implements PrintService {

    private static final int DEFAULT_MAX_RETRY = 3;

    private static final List<String> ACTIVE_STATUSES = Arrays.asList(
            PrintTaskStatus.PENDING.name(),
            PrintTaskStatus.PRINTING.name()
    );

    private final PrintTaskMapper printTaskMapper;
    private final PrintRecordMapper printRecordMapper;
    private final EquipmentService equipmentService;

    @Override
    @Transactional
    public void submitPrintTask(Long taskId, String deviceCode, String operatorId) {
        // 1. 幂等性检查：同一 taskId 已有进行中的打印任务则直接返回
        PrintTask existing = findActiveByTaskId(taskId);
        if (existing != null) {
            log.info("幂等返回: taskId={} 已有进行中的打印任务 prtTaskId={}, status={}",
                    taskId, existing.getId(), existing.getStatus());
            return;
        }

        // 2. 校验打印机
        EqDeviceDTO device = equipmentService.getOrCreateDevice(deviceCode, 4);
        if (device == null) {
            throw new IllegalStateException("打印机不存在或创建失败");
        }
        Long deviceId = device.getId();
        String deviceStatus = equipmentService.getDeviceStatus(deviceId);
        if (!"IDLE".equalsIgnoreCase(deviceStatus)) {
            throw new IllegalStateException("打印机不是空闲状态");
        }

        // 3. 创建打印任务并执行
        doPrint(taskId, deviceId, deviceCode, operatorId);
    }

    @Override
    @Transactional
    public void retryPrint(Long taskId, String deviceCode, String operatorId) {
        // 1. 查询该任务最新的打印记录
        PrintTask lastPrintTask = findLatestByTaskId(taskId);
        if (lastPrintTask == null) {
            throw new IllegalArgumentException("该任务无打印记录，无法重试: taskId=" + taskId);
        }

        // 2. 重试闭环：校验是否已达 maxRetry 上限
        int currentRetry = lastPrintTask.getRetryCount() != null ? lastPrintTask.getRetryCount() : 0;
        int maxRetry = lastPrintTask.getMaxRetry() != null ? lastPrintTask.getMaxRetry() : DEFAULT_MAX_RETRY;

        if (currentRetry >= maxRetry) {
            // 超过上限，固定为 FAILED 终态
            lastPrintTask.setStatus(PrintTaskStatus.FAILED.name());
            printTaskMapper.updateById(lastPrintTask);

            // 记录失败记录
            PrintRecord record = buildPrintRecord(lastPrintTask.getId(), deviceCode, "FAILED",
                    "重试次数已达上限(" + maxRetry + ")，进入终态 FAILED", operatorId);
            printRecordMapper.insert(record);

            log.warn("重试次数已达上限({}), taskId={}, prtTaskId={}", maxRetry, taskId, lastPrintTask.getId());
            throw new IllegalStateException("重试次数已达上限(" + maxRetry + ")，无法继续重试");
        }

        // 3. 更新重试计数并回退为 PENDING
        lastPrintTask.setRetryCount(currentRetry + 1);
        lastPrintTask.setStatus(PrintTaskStatus.PENDING.name());
        lastPrintTask.setDeviceCode(deviceCode);
        lastPrintTask.setOperatorId(operatorId);
        printTaskMapper.updateById(lastPrintTask);

        // 4. 校验打印机状态
        EqDeviceDTO device = equipmentService.getOrCreateDevice(deviceCode, 4);
        if (device == null) {
            throw new IllegalStateException("打印机不存在或创建失败");
        }
        Long deviceId = device.getId();
        String deviceStatus = equipmentService.getDeviceStatus(deviceId);
        if (!"IDLE".equalsIgnoreCase(deviceStatus)) {
            throw new IllegalStateException("打印机不是空闲状态");
        }

        // 5. 执行打印（使用已有的 printTask，不新建）
        executePrint(lastPrintTask, deviceId, deviceCode, operatorId);

        log.info("打印任务重试成功: prtTaskId={}, taskId={}, retryCount={}/{}",
                lastPrintTask.getId(), taskId, currentRetry + 1, maxRetry);
    }

    @Override
    public String getPrintStatus(Long taskId) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getTaskId, taskId)
               .orderByDesc(PrintTask::getCreatedAt)
               .last("LIMIT 1");
        PrintTask task = printTaskMapper.selectOne(wrapper);
        return task != null ? task.getStatus() : null;
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

    @Override
    public IPage<PrintTaskDTO> getPrintTasks(int page, int size) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PrintTask::getCreatedAt);
        IPage<PrintTask> entityPage = printTaskMapper.selectPage(new Page<>(page, size), wrapper);
        return entityPage.convert(this::toDTO);
    }

    /**
     * 首次打印：创建 PrintTask 记录并执行打印。
     */
    private void doPrint(Long taskId, Long deviceId, String deviceCode, String operatorId) {
        // 创建打印任务记录
        PrintTask printTask = new PrintTask();
        printTask.setTaskId(taskId);
        printTask.setDeviceCode(deviceCode);
        printTask.setOperatorId(operatorId);
        printTask.setStatus(PrintTaskStatus.PENDING.name());
        printTask.setCopies(1);
        printTask.setRetryCount(0);
        printTask.setMaxRetry(DEFAULT_MAX_RETRY);
        printTaskMapper.insert(printTask);

        executePrint(printTask, deviceId, deviceCode, operatorId);
    }

    /**
     * 执行打印：占用打印机 → 模拟打印 → 释放打印机 → 记录结果。
     */
    private void executePrint(PrintTask printTask, Long deviceId, String deviceCode, String operatorId) {
        // 占用打印机
        equipmentService.updateDeviceStatus(deviceId, "RUNNING");

        // 执行打印（同步模拟）
        printTask.setStatus(PrintTaskStatus.COMPLETED.name());
        printTaskMapper.updateById(printTask);

        // 释放打印机
        equipmentService.releaseDevice(deviceId);

        // 记录打印结果（含 operatorId 和 printerCode）
        PrintRecord record = buildPrintRecord(printTask.getId(), deviceCode, "SUCCESS", null, operatorId);
        printRecordMapper.insert(record);

        log.info("打印标签成功: taskId={}, deviceCode={}, operatorId={}", printTask.getTaskId(), deviceCode, operatorId);
    }

    private PrintRecord buildPrintRecord(Long printTaskId, String printerCode, String result,
                                         String errorMessage, String operatorId) {
        PrintRecord record = new PrintRecord();
        record.setPrintTaskId(printTaskId);
        record.setPrinterCode(printerCode);
        record.setResult(result);
        record.setErrorMessage(errorMessage);
        record.setOperatorId(operatorId);
        record.setPrintedAt(LocalDateTime.now());
        return record;
    }

    private PrintTask findActiveByTaskId(Long taskId) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getTaskId, taskId)
               .in(PrintTask::getStatus, ACTIVE_STATUSES)
               .orderByDesc(PrintTask::getCreatedAt)
               .last("LIMIT 1");
        return printTaskMapper.selectOne(wrapper);
    }

    private PrintTask findLatestByTaskId(Long taskId) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTask::getTaskId, taskId)
               .orderByDesc(PrintTask::getCreatedAt)
               .last("LIMIT 1");
        return printTaskMapper.selectOne(wrapper);
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
        dto.setMaxRetry(task.getMaxRetry());
        dto.setCreatedAt(task.getCreatedAt());
        // 查询打印机设备类型
        if (task.getDeviceCode() != null) {
            try {
                cn.org.openygt.common.dto.EqDeviceDTO device = equipmentService.getDeviceByCode(task.getDeviceCode());
                if (device != null && device.getDeviceType() != null) {
                    String type = device.getDeviceType();
                    if ("3".equals(type)) {
                        dto.setPrintType("标签打印");
                    } else if ("4".equals(type)) {
                        dto.setPrintType("激光打印");
                    } else {
                        dto.setPrintType("未知");
                    }
                }
            } catch (Exception ignored) {}
        }
        return dto;
    }
}
