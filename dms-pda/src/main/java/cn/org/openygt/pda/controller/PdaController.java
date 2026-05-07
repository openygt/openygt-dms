package cn.org.openygt.pda.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.ChangePasswordRequest;
import cn.org.openygt.common.dto.LoginRequest;
import cn.org.openygt.common.dto.TokenResponse;
import cn.org.openygt.pda.dto.*;
import cn.org.openygt.pda.entity.PdaLoginRecord;
import cn.org.openygt.pda.entity.PdaOperationLog;
import cn.org.openygt.pda.entity.PdaReviewPhoto;
import cn.org.openygt.pda.service.PdaGatewaySessionService;
import cn.org.openygt.pda.service.PdaLoginRecordService;
import cn.org.openygt.pda.service.PdaOperationLogService;
import cn.org.openygt.pda.service.PdaReviewPhotoService;
import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.entity.PrescriptionMedicine;
import cn.org.openygt.production.entity.StepLog;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.mapper.PrescriptionMedicineMapper;
import cn.org.openygt.production.service.TaskService;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.system.service.SysUserService;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.entity.EqDevicePairing;
import cn.org.openygt.equipment.service.DecoctionTraceService;
import cn.org.openygt.equipment.service.EqDevicePairingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/pda")
@RequiredArgsConstructor
@Validated
public class PdaController {

    private final PdaLoginRecordService loginRecordService;
    private final PdaReviewPhotoService reviewPhotoService;
    private final PdaOperationLogService operationLogService;
    private final TaskService taskService;
    private final SysUserService sysUserService;
    private final PdaGatewaySessionService pdaGatewaySessionService;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionMedicineMapper prescriptionMedicineMapper;
    private final EquipmentService equipmentService;
    private final DecoctionTraceService decoctionTraceService;
    private final EqDevicePairingService eqDevicePairingService;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    // ==================== 认证 ====================

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Validated PdaLoginRequest request,
                                                   HttpServletRequest httpRequest) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getUserCode());
        loginRequest.setPassword(request.getPassword());
        TokenResponse tokenResponse;
        try {
            tokenResponse = sysUserService.login(loginRequest);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("PDA登录失败: userCode={}, error={}", request.getUserCode(), e.getMessage());
            return ApiResponse.error(401, e.getMessage());
        }

        Long userId = tokenResponse.getUserId();
        String token = tokenResponse.getToken();
        SysUser user = sysUserService.getByUsername(request.getUserCode());
        EqDeviceDTO device = resolveDeviceByMac(request.getMacAddress());
        if (device == null) {
            return ApiResponse.error(400, "未找到对应PDA设备，请先配置设备通信ID为MAC地址");
        }
        unregisterGatewaySessionIfNeeded(loginRecordService.getLatestOnlineByUserId(userId));
        unregisterGatewaySessionIfNeeded(loginRecordService.getLatestOnlineByDeviceCode(device.getDeviceCode()));

        PdaLoginRecord record = loginRecordService.login(
                userId, request.getUserCode(), device.getId(),
                device.getDeviceCode(), httpRequest.getRemoteAddr());
        pdaGatewaySessionService.register(request.getMacAddress(), device.getDeviceCode(),
                userId, request.getUserCode(), user != null ? user.getRealName() : request.getUserCode());

        Map<String, Object> result = buildLoginResult(token, userId, request.getUserCode(),
                user != null ? user.getRealName() : request.getUserCode(),
                device.getDeviceCode(), record.getId(), tokenResponse);
        result.put("deviceId", device.getId());
        result.put("macAddress", normalizeMac(request.getMacAddress()));

        operationLogService.logOperation(userId, request.getUserCode(), device.getId(),
                device.getDeviceCode(), null, "LOGIN", "PDA账号登录",
                "SUCCESS", "/api/v1/pda/auth/login", "POST",
                httpRequest.getRemoteAddr(), null);

        return ApiResponse.success(result);
    }

    @PostMapping("/auth/scan-login")
    public ApiResponse<Map<String, Object>> scanLogin(@RequestBody @Validated PdaScanLoginRequest request,
                                                       HttpServletRequest httpRequest) {
        SysUser user = sysUserService.getByBarcode(request.getScanCode());
        if (user == null) {
            return ApiResponse.error(401, "员工码不存在: " + request.getScanCode());
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            return ApiResponse.error(401, "用户已被禁用");
        }

        // 扫码登录免密码，直接生成 token
        List<String> roles = resolveUserRoles(user);
        List<String> permissions = new ArrayList<>(roles);
        String token = cn.org.openygt.common.util.JwtUtil.generateToken(
                user.getId(), user.getUsername(), roles, permissions);
        EqDeviceDTO device = resolveDeviceByMac(request.getMacAddress());
        if (device == null) {
            return ApiResponse.error(400, "未找到对应PDA设备，请先配置设备通信ID为MAC地址");
        }
        unregisterGatewaySessionIfNeeded(loginRecordService.getLatestOnlineByUserId(user.getId()));
        unregisterGatewaySessionIfNeeded(loginRecordService.getLatestOnlineByDeviceCode(device.getDeviceCode()));

        PdaLoginRecord record = loginRecordService.login(
                user.getId(), user.getUsername(), device.getId(),
                device.getDeviceCode(), httpRequest.getRemoteAddr());
        pdaGatewaySessionService.register(request.getMacAddress(), device.getDeviceCode(),
                user.getId(), user.getUsername(), user.getRealName());

        TokenResponse scanTokenResponse = new TokenResponse();
        scanTokenResponse.setToken(token);
        scanTokenResponse.setRoles(roles);
        Map<String, Object> result = buildLoginResult(token, user.getId(), user.getUsername(),
                user.getRealName(), device.getDeviceCode(), record.getId(), scanTokenResponse);
        result.put("deviceId", device.getId());
        result.put("macAddress", normalizeMac(request.getMacAddress()));

        operationLogService.logOperation(user.getId(), user.getUsername(), device.getId(),
                device.getDeviceCode(), null, "LOGIN", "PDA扫码登录",
                "SUCCESS", "/api/v1/pda/auth/scan-login", "POST",
                httpRequest.getRemoteAddr(), null);

        return ApiResponse.success(result);
    }

    private Map<String, Object> buildLoginResult(String token, Long userId, String userCode,
                                                  String userName, String deviceCode,
                                                  Long recordId, TokenResponse tokenResponse) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", userId);
        result.put("recordId", recordId);
        result.put("userCode", userCode);
        result.put("userName", userName);
        result.put("deviceCode", deviceCode);
        if (tokenResponse != null) {
            result.put("permissions", tokenResponse.getRoles() != null ? tokenResponse.getRoles() : Collections.emptyList());
        } else {
            result.put("permissions", Collections.emptyList());
        }
        return result;
    }

    private List<String> resolveUserRoles(SysUser user) {
        if (user == null || user.getRole() == null || user.getRole().trim().isEmpty()) {
            return Collections.emptyList();
        }
        switch (user.getRole().trim()) {
            case "主任":
                return Collections.singletonList("ROLE_DIRECTOR");
            case "班长":
                return Collections.singletonList("ROLE_LEADER");
            case "煎药工":
                return Collections.singletonList("ROLE_WORKER");
            case "质检员":
                return Collections.singletonList("ROLE_INSPECTOR");
            default:
                return Collections.emptyList();
        }
    }

    @PostMapping("/auth/logout")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Boolean> logout(@RequestAttribute("userId") Long userId) {
        PdaLoginRecord record = loginRecordService.getLatestOnlineByUserId(userId);
        boolean success = loginRecordService.logoutByUserId(userId);
        if (success && record != null) {
            unregisterGatewaySessionIfNeeded(record);
        }
        return ApiResponse.success(success);
    }

    @PostMapping("/auth/change-password")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Void> changePassword(@RequestAttribute("userId") Long userId,
                                             @RequestBody @Validated ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.error(400, "两次输入的新密码不一致");
        }
        sysUserService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success();
    }

    // ==================== 版本检查 ====================

    @GetMapping("/config/version")
    public ApiResponse<Map<String, Object>> getVersion() {
        Map<String, Object> result = new HashMap<>();
        result.put("version", appVersion);
        result.put("forceUpdate", false);
        result.put("downloadUrl", "");
        return ApiResponse.success(result);
    }

    // ==================== 任务 ====================

    @GetMapping("/task/{barcode}")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> queryTaskByBarcode(@PathVariable String barcode) {
        Task task = taskService.getByBarcode(barcode);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在: " + barcode);
        }
        return ApiResponse.success(buildTaskDetail(task));
    }

    @GetMapping("/task/{barcode}/temperature-curve")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> getTaskTemperatureCurve(@PathVariable String barcode) {
        Task task = taskService.getByBarcode(barcode);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在: " + barcode);
        }
        Prescription prescription = prescriptionMapper.selectById(task.getPrescriptionId());
        String prescriptionNo = prescription != null ? prescription.getPrescriptionNumber() : null;
        Map<String, Object> curveData = new HashMap<>();
        if (prescriptionNo != null) {
            curveData = decoctionTraceService.getTemperatureCurve(prescriptionNo);
        }
        return ApiResponse.success(curveData);
    }

    @GetMapping("/tasks/recent")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<List<Map<String, Object>>> queryRecentTasks(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "20") Integer limit) {
        // 查询当前操作人正在处理的任务（排除已完成/已报废）
        List<Task> tasks = taskService.queryTasks(null, null, null, null,
                String.valueOf(userId), null, null, null, null, 1, limit).getRecords().stream()
                .filter(t -> !"已完成".equals(t.getStatus()) && !"已报废".equals(t.getStatus()))
                .collect(Collectors.toList());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> item = new HashMap<>();
            item.put("taskId", task.getId());
            item.put("barcode", task.getBarcode());
            item.put("status", mapStatus(task.getStatus()));
            item.put("statusName", task.getStatus());
            item.put("patientName", getPatientName(task));
            item.put("updatedAt", task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : task.getCreatedAt().toString());
            // 补充处方号
            if (task.getPrescriptionId() != null) {
                Prescription prescription = prescriptionMapper.selectById(task.getPrescriptionId());
                if (prescription != null) {
                    item.put("prescriptionNumber", prescription.getPrescriptionNumber());
                }
            }
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/task/confirm")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> confirmTaskStep(@RequestBody @Validated PdaTaskConfirmRequest request,
                                                             @RequestAttribute("userId") Long userId,
                                                             @RequestAttribute("username") String username,
                                                             HttpServletRequest httpRequest) {
        String operatorId = String.valueOf(userId);
        String stepType = request.getStepType();
        Long taskId = request.getTaskId();

        try {
            Task task;
            switch (stepType) {
                case "START_SOAK":
                    task = taskService.startSoak(taskId, operatorId);
                    break;
                case "END_SOAK":
                    task = taskService.endSoak(taskId, operatorId);
                    break;
                case "START_DECOCT":
                    Long decoctDeviceId = request.getDeviceId();
                    String decoctDeviceCode = decoctDeviceId != null ? equipmentService.getDeviceCode(decoctDeviceId) : null;
                    task = taskService.startDecoct(taskId, decoctDeviceCode, operatorId);
                    break;
                case "END_DECOCT":
                    task = taskService.endDecoct(taskId, operatorId);
                    break;
                case "START_POUR":
                    task = taskService.startPour(taskId, operatorId);
                    break;
                case "END_POUR":
                    task = taskService.endPour(taskId, operatorId);
                    break;
                case "START_WRAP":
                case "START_PACKAGE":
                    String packageDeviceCode = resolvePackageDevice(request.getDeviceId(), taskId);
                    task = taskService.startWrap(taskId, packageDeviceCode, operatorId);
                    break;
                case "END_WRAP":
                case "END_PACKAGE":
                    task = taskService.endWrap(taskId, operatorId);
                    break;
                case "LABEL_CONFIRM":
                    task = taskService.confirmLabel(taskId, operatorId);
                    break;
                case "INSPECT_PASS":
                    task = taskService.qualityInspect(taskId, cn.org.openygt.common.enums.InspectionResultType.PASS, operatorId, null, null);
                    break;
                default:
                    return ApiResponse.error(400, "不支持的工序类型: " + stepType);
            }

            // 更新操作人姓名
            task.setOperatorName(username);
            taskService.updateTemperature(task.getBarcode(), task.getCurrentTemp()); // hack: 触发 update

            operationLogService.logOperation(userId, username, request.getDeviceId(), null,
                    taskId, "TASK_CONFIRM", "工序确认:" + stepType,
                    "SUCCESS", "/api/v1/pda/task/confirm", "POST",
                    httpRequest.getRemoteAddr(), null);

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("newStatus", mapStatus(task.getStatus()));
            result.put("newStatusName", task.getStatus());
            result.put("stepType", stepType);
            return ApiResponse.success(result);
        } catch (IllegalStateException e) {
            log.warn("工序确认状态非法: taskId={}, stepType={}, error={}", taskId, stepType, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("工序确认失败: taskId={}, stepType={}", taskId, stepType, e);
            return ApiResponse.error(500, "工序确认失败: " + e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/bind-device")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> bindDevice(@PathVariable Long taskId,
                                                        @RequestBody @Validated PdaBindDeviceRequest request,
                                                        @RequestAttribute("userId") Long userId,
                                                        @RequestAttribute("username") String username,
                                                        HttpServletRequest httpRequest) {
        try {
            Task task = taskService.bindDevice(taskId, request.getDeviceCode());
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("deviceCode", request.getDeviceCode());
            result.put("bindTime", task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : "");

            operationLogService.logOperation(userId, username, null, request.getDeviceCode(),
                    taskId, "DEVICE_BIND", "设备绑定:" + request.getDeviceCode(),
                    "SUCCESS", "/api/v1/pda/tasks/" + taskId + "/bind-device", "POST",
                    httpRequest.getRemoteAddr(), null);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/sign")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> handoverSign(@PathVariable Long taskId,
                                                          @RequestBody PdaTaskSignRequest request,
                                                          @RequestAttribute("userId") Long userId,
                                                          @RequestAttribute("username") String username,
                                                          HttpServletRequest httpRequest) {
        try {
            Task task = taskService.handover(taskId, null,
                    request.getSignType() != null ? request.getSignType() : "HANDOVER",
                    request.getHandoverTo() != null ? request.getHandoverTo() : username,
                    request.getRemark(), true);
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", mapStatus(task.getStatus()));
            result.put("signType", request.getSignType());
            return ApiResponse.success(result);
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/reprint")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> reprintTask(@PathVariable Long taskId,
                                                         @RequestBody PdaTaskReprintRequest request,
                                                         @RequestAttribute("userId") Long userId,
                                                         @RequestAttribute("username") String username,
                                                         HttpServletRequest httpRequest) {
        try {
            // 重打印使用 retryPrint 方法
            Task task = taskService.retryPrint(taskId, null, String.valueOf(userId));
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("printJobId", taskId);
            result.put("status", "PRINTING");
            result.put("message", "打印任务已下发");
            return ApiResponse.success(result);
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // ==================== 照片 ====================

    @PostMapping("/photo/upload")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<PdaReviewPhoto> uploadPhoto(@RequestBody @Validated PdaPhotoUploadRequest request,
                                                    @RequestAttribute("userId") Long userId,
                                                    @RequestAttribute("username") String username) {
        PdaReviewPhoto photo = reviewPhotoService.uploadPhoto(
                request.getTaskId(), request.getPrescriptionId(), request.getPhotoUrl(),
                request.getPhotoType(), request.getFileSize(), userId, username,
                request.getRemark());
        return ApiResponse.success(photo);
    }

    @GetMapping("/photo/task/{taskId}")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<List<PdaReviewPhoto>> listPhotosByTask(@PathVariable Long taskId) {
        return ApiResponse.success(reviewPhotoService.listByTaskId(taskId));
    }

    // ==================== 日志 ====================

    @PostMapping("/log/operate")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<PdaOperationLog> logOperation(@RequestBody PdaOperationLog log,
                                                      @RequestAttribute("userId") Long userId,
                                                      @RequestAttribute("username") String username,
                                                      HttpServletRequest httpRequest) {
        log.setUserId(userId);
        log.setUserName(username);
        log.setClientIp(httpRequest.getRemoteAddr());
        operationLogService.save(log);
        return ApiResponse.success(log);
    }

    @GetMapping("/log/recent")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<List<PdaOperationLog>> listRecentLogs(@RequestAttribute("userId") Long userId,
                                                              @RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.success(operationLogService.listRecentByUserId(userId, limit));
    }

    @GetMapping("/online/list")
    @RequiresPermissions({"ROLE_LEADER", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<List<PdaLoginRecord>> listOnlineUsers() {
        return ApiResponse.success(loginRecordService.listAllOnline());
    }

    // ==================== 私有辅助方法 ====================

    private Map<String, Object> buildTaskDetail(Task task) {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("barcode", task.getBarcode());
        result.put("status", mapStatus(task.getStatus()));
        result.put("statusName", task.getStatus());
        result.put("currentStep", task.getCurrentStep());
        result.put("operatorId", task.getOperatorId());
        result.put("operatorName", task.getOperatorName());
        result.put("currentTemp", task.getCurrentTemp());
        result.put("targetTemp", task.getTargetTemp());
        result.put("currentStageDuration", task.getCurrentStageDuration());

        // 设备信息
        if (task.getDecoctDeviceId() != null) {
            result.put("decoctDeviceId", task.getDecoctDeviceId());
            cn.org.openygt.common.dto.EqDeviceDTO device = equipmentService.getDeviceById(task.getDecoctDeviceId());
            if (device != null) {
                result.put("deviceCode", device.getDeviceCode());
                result.put("deviceName", device.getName());
            }
        }
        if (task.getPackageDeviceId() != null) {
            result.put("packageDeviceId", task.getPackageDeviceId());
        }

        // 关联处方信息
        if (task.getPrescriptionId() != null) {
            Prescription prescription = prescriptionMapper.selectById(task.getPrescriptionId());
            if (prescription != null) {
                result.put("patientName", prescription.getPatientName());
                result.put("prescriptionNumber", prescription.getPrescriptionNumber());
                result.put("repetition", prescription.getRepetition());
                result.put("hospitalName", prescription.getHospitalName());
                result.put("prescriptionName", prescription.getDisease());
            }
            // 关联药材清单
            List<PrescriptionMedicine> medicines = prescriptionMedicineMapper.selectByPrescriptionId(task.getPrescriptionId());
            if (medicines != null && !medicines.isEmpty()) {
                result.put("medicines", medicines.stream().map(m -> {
                    Map<String, Object> med = new HashMap<>();
                    med.put("name", m.getMedicineName());
                    med.put("dosage", m.getDosage());
                    med.put("unit", m.getUnit());
                    med.put("decoctionMethod", m.getDecoctionMethod());
                    return med;
                }).collect(Collectors.toList()));
            }
        }

        // 工序时间线
        List<StepLog> stepLogs = taskService.queryStepLogs(task.getId());
        result.put("steps", buildStepTimeline(task.getStatus(), stepLogs));

        // 下一步操作
        result.put("nextAction", buildNextAction(task.getStatus()));

        return result;
    }

    private EqDeviceDTO resolveDeviceByMac(String macAddress) {
        return equipmentService.getDeviceByCommunicationId(normalizeMac(macAddress));
    }

    private String normalizeMac(String macAddress) {
        return macAddress == null ? null : macAddress.trim().toUpperCase();
    }

    private void unregisterGatewaySessionIfNeeded(PdaLoginRecord record) {
        if (record == null || record.getDeviceCode() == null || record.getDeviceCode().trim().isEmpty()) {
            return;
        }
        EqDeviceDTO device = equipmentService.getDeviceByCode(record.getDeviceCode());
        if (device == null || device.getCommunicationId() == null || device.getCommunicationId().trim().isEmpty()) {
            return;
        }
        pdaGatewaySessionService.unregister(device.getCommunicationId());
    }
    private String getPatientName(Task task) {
        if (task.getPrescriptionId() != null) {
            Prescription p = prescriptionMapper.selectById(task.getPrescriptionId());
            if (p != null) return p.getPatientName();
        }
        return "-";
    }

    private List<Map<String, Object>> buildStepTimeline(String status, List<StepLog> stepLogs) {
        String[] stepLabels = {"开始泡药", "结束泡药", "开始煎药", "结束煎药", "开始出液", "结束出液", "开始包装", "结束包装", "贴标确认", "质检通过"};
        String[] stepValues = {"START_SOAK", "END_SOAK", "START_DECOCT", "END_DECOCT", "START_POUR", "END_POUR", "START_PACKAGE", "END_PACKAGE", "LABEL_CONFIRM", "INSPECT_PASS"};
        String[] stepGroups = {"SOAK", "SOAK", "DECOCT", "DECOCT", "POUR", "POUR", "PACKAGE", "PACKAGE", "LABEL", "INSPECT"};
        boolean[] needDevices = {false, false, true, false, false, false, true, false, false, false};
        boolean[] needPhotos = {false, false, false, false, false, false, false, false, false, true};

        // 计算当前步骤索引
        int currentIndex = getCurrentStepIndex(status);

        List<Map<String, Object>> steps = new ArrayList<>();
        for (int i = 0; i < stepLabels.length; i++) {
            Map<String, Object> step = new HashMap<>();
            step.put("value", stepValues[i]);
            step.put("label", stepLabels[i]);
            step.put("completed", i < currentIndex);
            step.put("current", i == currentIndex);

            // 查找对应的工序记录
            final String currentStepValue = stepValues[i];
            StepLog log = stepLogs.stream()
                    .filter(s -> currentStepValue.equals(s.getStepType()))
                    .findFirst().orElse(null);
            step.put("completedAt", log != null && log.getEndedAt() != null ? log.getEndedAt().toString() : null);
            step.put("operatorName", log != null ? log.getOperatorId() : null);
            step.put("needDevice", needDevices[i]);
            step.put("needPhoto", needPhotos[i]);
            steps.add(step);
        }
        return steps;
    }

    private Map<String, Object> buildNextAction(String status) {
        Map<String, String> nextMap = new HashMap<>();
        nextMap.put("待泡药", "START_SOAK");
        nextMap.put("泡药中", "END_SOAK");
        nextMap.put("待煎药", "START_DECOCT");
        nextMap.put("煎药中", "END_DECOCT");
        nextMap.put("待出液", "START_POUR");
        nextMap.put("出液中", "END_POUR");
        nextMap.put("待包装", "START_PACKAGE");
        nextMap.put("包装中", "END_PACKAGE");
        nextMap.put("待贴标", "LABEL_CONFIRM");
        nextMap.put("待质检", "INSPECT_PASS");
        nextMap.put("待交接", "HANDOVER_SIGN");

        String stepType = nextMap.get(status);
        if (stepType == null) return null;

        // 特殊处理交接签字
        if ("HANDOVER_SIGN".equals(stepType)) {
            Map<String, Object> action = new HashMap<>();
            action.put("text", "交接签字");
            action.put("stepType", "HANDOVER_SIGN");
            action.put("needDevice", false);
            action.put("needPhoto", false);
            return action;
        }

        String[] labels = {"开始泡药", "结束泡药", "开始煎药", "结束煎药", "开始出液", "结束出液", "开始包装", "结束包装", "贴标确认", "质检通过"};
        String[] values = {"START_SOAK", "END_SOAK", "START_DECOCT", "END_DECOCT", "START_POUR", "END_POUR", "START_PACKAGE", "END_PACKAGE", "LABEL_CONFIRM", "INSPECT_PASS"};
        boolean[] needDevices = {false, false, true, false, false, false, true, false, false, false};
        boolean[] needPhotos = {false, false, false, false, false, false, false, false, false, true};

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(stepType)) {
                Map<String, Object> action = new HashMap<>();
                action.put("text", labels[i]);
                action.put("stepType", stepType);
                action.put("needDevice", needDevices[i]);
                action.put("needPhoto", needPhotos[i]);
                return action;
            }
        }
        return null;
    }

    private int getCurrentStepIndex(String status) {
        switch (status) {
            case "待泡药": return 0;
            case "泡药中": return 1;
            case "待煎药": return 2;
            case "煎药中": return 3;
            case "待出液": return 4;
            case "出液中": return 5;
            case "待包装": return 6;
            case "包装中": return 7;
            case "待贴标": return 8;
            case "待质检": return 9;
            case "待交接": return 10;
            case "已完成":
            case "已部分完成": return 10;
            case "已报废": return -1;
            default: return -1;
        }
    }

    private String mapStatus(String dbStatus) {
        switch (dbStatus) {
            case "待泡药": return "PENDING";
            case "泡药中": return "SOAKING";
            case "待煎药": return "SOAKED";
            case "煎药中": return "DECOCTING";
            case "待出液": return "DECOCTED";
            case "出液中": return "POURING";
            case "待包装": return "POURED";
            case "包装中": return "PACKAGING";
            case "待贴标": return "PACKAGED";
            case "待质检": return "LABELING";
            case "待交接": return "INSPECTING";
            case "已完成":
            case "已部分完成": return "COMPLETED";
            case "已报废": return "CANCELLED";
            default: return dbStatus;
        }
    }

    /**
     * 解析包装机设备编码：根据设备分组自动匹配包装机
     */
    private String resolvePackageDevice(Long deviceId, Long taskId) {
        // 1. 如果传入了包装机ID，直接使用
        if (deviceId != null) {
            EqDeviceDTO device = equipmentService.getDeviceById(deviceId);
            if (device != null && "2".equals(device.getDeviceType())) {
                return device.getDeviceCode();
            }
        }

        // 2. 从配对表查找：用煎药机ID匹配对应的包装机
        Long decoctId = deviceId;
        if (decoctId == null) {
            Task task = taskService.getById(taskId);
            if (task != null) {
                decoctId = task.getDecoctDeviceId();
            }
        }

        if (decoctId != null) {
            EqDevicePairing pairing = eqDevicePairingService.findByDecocterId(decoctId);
            if (pairing != null && pairing.getPackerId() != null) {
                return equipmentService.getDeviceCode(pairing.getPackerId());
            }
        }

        // 3. 降级：直接使用传入的deviceId
        if (deviceId != null) {
            return equipmentService.getDeviceCode(deviceId);
        }

        return null;
    }
}
