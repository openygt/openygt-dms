package cn.org.openygt.pda.service;

import cn.org.openygt.common.enums.TaskStatus;
import cn.org.openygt.common.enums.TaskStatusDisplay;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * PDA 统一扫码处理器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdaScanHandler {

    private final TaskService taskService;

    public PdaScanResult handleScan(String barcode, Long operatorId) {
        BarcodeType type = parseBarcodeType(barcode);
        switch (type) {
            case TASK:
                return handleTaskScan(barcode, operatorId);
            case DEVICE:
                return handleDeviceScan(barcode, operatorId);
            case MEDICINE:
                return handleMedicineScan(barcode, operatorId);
            case UNKNOWN:
            default:
                return PdaScanResult.error("无法识别条码类型");
        }
    }

    private BarcodeType parseBarcodeType(String barcode) {
        if (barcode == null || barcode.isEmpty()) return BarcodeType.UNKNOWN;
        // 优先查数据库判断条码类型：先查任务，再查设备
        if (taskService.getByBarcode(barcode) != null) return BarcodeType.TASK;
        // 设备条码前缀识别（TODO: 未来可改为查设备表）
        if (barcode.startsWith("DEV") || barcode.startsWith("EQ")) return BarcodeType.DEVICE;
        return BarcodeType.MEDICINE;
    }

    private PdaScanResult handleTaskScan(String barcode, Long operatorId) {
        Task task = taskService.getByBarcode(barcode);
        if (task == null) {
            return PdaScanResult.error("任务不存在");
        }
        TaskStatus status = TaskStatus.fromLabel(task.getStatus());
        if (status == null) {
            // 兼容英文枚举名（如 COMPLETED）
            try {
                status = TaskStatus.valueOf(task.getStatus());
            } catch (IllegalArgumentException e) {
                return PdaScanResult.info("当前状态：" + task.getStatus(), task);
            }
        }
        switch (status) {
            case WAIT_SOAK:
                taskService.startSoak(task.getId(), String.valueOf(operatorId));
                return PdaScanResult.success("泡药已开始", task);
            case WAIT_DECOCT:
                return PdaScanResult.success("待煎药，请绑定设备", task);
            case WAIT_POUR:
                taskService.startPour(task.getId(), String.valueOf(operatorId));
                return PdaScanResult.success("出液已开始", task);
            case WAIT_WRAP:
                return PdaScanResult.prompt("待包装，请先扫码绑定包装设备", task, "wrap-device");
            case WAIT_QC:
                return PdaScanResult.prompt("请录入质检结果", task, "qc-form");
            case STORED:
            case WAIT_HANDOVER:
                return PdaScanResult.prompt("请确认交接", task, "handover-form");
            case SUSPENDED:
                return PdaScanResult.warning("任务已挂起：" + task.getSuspendReason(), task);
            case COMPLETED:
                return PdaScanResult.info("任务已完成", task);
            case SCRAPPED:
                return PdaScanResult.warning("任务已报废", task);
            default:
                return PdaScanResult.info("当前状态：" + task.getStatus(), task);
        }
    }

    private PdaScanResult handleDeviceScan(String barcode, Long operatorId) {
        return PdaScanResult.info("设备扫码功能待实现", null);
    }

    private PdaScanResult handleMedicineScan(String barcode, Long operatorId) {
        return PdaScanResult.info("药材扫码功能待实现", null);
    }

    public enum BarcodeType {
        TASK, DEVICE, MEDICINE, UNKNOWN
    }

    public static class PdaScanResult {
        private boolean success;
        private String action;
        private String actionName;
        private Long taskId;
        private String taskStatus;
        private DisplayInfo display;
        private String nextAction;
        private String errorMsg;

        public PdaScanResult(boolean success, String action, String actionName,
                             Long taskId, String taskStatus, DisplayInfo display,
                             String nextAction, String errorMsg) {
            this.success = success;
            this.action = action;
            this.actionName = actionName;
            this.taskId = taskId;
            this.taskStatus = taskStatus;
            this.display = display;
            this.nextAction = nextAction;
            this.errorMsg = errorMsg;
        }

        public static PdaScanResult success(String actionName, Task task) {
            return new PdaScanResult(true, "AUTO_EXECUTED", actionName,
                    task != null ? task.getId() : null,
                    task != null ? task.getStatus() : null,
                    task != null ? buildDisplay(task) : null, null, null);
        }

        public static PdaScanResult info(String actionName, Task task) {
            return new PdaScanResult(true, "INFO", actionName,
                    task != null ? task.getId() : null,
                    task != null ? task.getStatus() : null,
                    task != null ? buildDisplay(task) : null, null, null);
        }

        public static PdaScanResult warning(String actionName, Task task) {
            return new PdaScanResult(true, "WARNING", actionName,
                    task != null ? task.getId() : null,
                    task != null ? task.getStatus() : null,
                    task != null ? buildDisplay(task) : null, null, null);
        }

        public static PdaScanResult prompt(String actionName, Task task, String formType) {
            return new PdaScanResult(true, "PROMPT", actionName,
                    task != null ? task.getId() : null,
                    task != null ? task.getStatus() : null,
                    task != null ? buildDisplay(task) : null, formType, null);
        }

        public static PdaScanResult error(String errorMsg) {
            return new PdaScanResult(false, "ERROR", null, null, null, null, null, errorMsg);
        }

        private static DisplayInfo buildDisplay(Task task) {
            TaskStatusDisplay display = TaskStatusDisplay.of(task.getStatus());
            Map<String, String> params = new HashMap<>();
            if (task.getSuspendReason() != null) {
                params.put("suspendReason", task.getSuspendReason());
            }
            return new DisplayInfo(
                    display != null ? display.getDisplayName() : task.getStatus(),
                    display != null ? display.getColor() : "#999",
                    display != null ? display.getIcon() : "",
                    display != null ? display.renderDescription(params) : task.getStatus()
            );
        }

        // getters
        public boolean isSuccess() { return success; }
        public String getAction() { return action; }
        public String getActionName() { return actionName; }
        public Long getTaskId() { return taskId; }
        public String getTaskStatus() { return taskStatus; }
        public DisplayInfo getDisplay() { return display; }
        public String getNextAction() { return nextAction; }
        public String getErrorMsg() { return errorMsg; }
    }

    public static class DisplayInfo {
        private String statusName;
        private String statusColor;
        private String statusIcon;
        private String description;

        public DisplayInfo(String statusName, String statusColor, String statusIcon, String description) {
            this.statusName = statusName;
            this.statusColor = statusColor;
            this.statusIcon = statusIcon;
            this.description = description;
        }

        public String getStatusName() { return statusName; }
        public String getStatusColor() { return statusColor; }
        public String getStatusIcon() { return statusIcon; }
        public String getDescription() { return description; }
    }
}
