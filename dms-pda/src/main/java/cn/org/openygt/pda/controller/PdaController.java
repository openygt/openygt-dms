package cn.org.openygt.pda.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.LoginRequest;
import cn.org.openygt.common.dto.TokenResponse;
import cn.org.openygt.common.util.JwtUtil;
import cn.org.openygt.pda.dto.PdaLoginRequest;
import cn.org.openygt.pda.dto.PdaPhotoUploadRequest;
import cn.org.openygt.pda.dto.PdaTaskConfirmRequest;
import cn.org.openygt.pda.entity.PdaLoginRecord;
import cn.org.openygt.pda.entity.PdaOperationLog;
import cn.org.openygt.pda.entity.PdaReviewPhoto;
import cn.org.openygt.pda.service.PdaLoginRecordService;
import cn.org.openygt.pda.service.PdaOperationLogService;
import cn.org.openygt.pda.service.PdaReviewPhotoService;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.service.TaskService;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import cn.org.openygt.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Validated PdaLoginRequest request,
                                                   HttpServletRequest httpRequest) {
        // 调用系统用户服务进行认证（userCode 映射为 username）
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

        PdaLoginRecord record = loginRecordService.login(
                userId, request.getUserCode(), request.getDeviceId(),
                request.getDeviceCode(), httpRequest.getRemoteAddr());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("recordId", record.getId());
        result.put("userCode", request.getUserCode());
        result.put("deviceCode", request.getDeviceCode());

        operationLogService.logOperation(userId, request.getUserCode(), request.getDeviceId(),
                request.getDeviceCode(), null, "LOGIN", "PDA登录",
                "SUCCESS", "/api/v1/pda/auth/login", "POST",
                httpRequest.getRemoteAddr(), null);

        return ApiResponse.success(result);
    }

    @PostMapping("/auth/logout")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Boolean> logout(@RequestAttribute("userId") Long userId) {
        boolean success = loginRecordService.logoutByUserId(userId);
        return ApiResponse.success(success);
    }

    @GetMapping("/task/{barcode}")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> queryTaskByBarcode(@PathVariable String barcode) {
        Task task = taskService.getByBarcode(barcode);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在: " + barcode);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("barcode", barcode);
        result.put("status", task.getStatus());
        result.put("currentStep", task.getCurrentStep());
        result.put("prescriptionId", task.getPrescriptionId());
        result.put("schemeId", task.getSchemeId());
        result.put("operatorId", task.getOperatorId());
        result.put("currentTemp", task.getCurrentTemp());
        result.put("targetTemp", task.getTargetTemp());
        result.put("currentStageDuration", task.getCurrentStageDuration());
        return ApiResponse.success(result);
    }

    @PostMapping("/task/confirm")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Boolean> confirmTaskStep(@RequestBody @Validated PdaTaskConfirmRequest request,
                                                 @RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("username") String username,
                                                 HttpServletRequest httpRequest) {
        String operatorId = String.valueOf(userId);
        String stepType = request.getStepType();
        Long taskId = request.getTaskId();
        
        try {
            switch (stepType) {
                case "START_SOAK":
                    taskService.startSoak(taskId, operatorId);
                    break;
                case "END_SOAK":
                    taskService.endSoak(taskId, operatorId);
                    break;
                case "START_DECOCT":
                    taskService.startDecoct(taskId, null, operatorId);
                    break;
                case "END_DECOCT":
                    taskService.endDecoct(taskId, operatorId);
                    break;
                case "START_POUR":
                    taskService.startPour(taskId, operatorId);
                    break;
                case "END_POUR":
                    taskService.endPour(taskId, operatorId);
                    break;
                case "START_PACKAGE":
                    taskService.startWrap(taskId, null, operatorId);
                    break;
                case "END_PACKAGE":
                    taskService.endWrap(taskId, operatorId);
                    break;
                case "LABEL_CONFIRM":
                    taskService.confirmLabel(taskId, operatorId);
                    break;
                default:
                    return ApiResponse.error(400, "不支持的工序类型: " + stepType);
            }
            
            operationLogService.logOperation(userId, username, request.getDeviceId(), null,
                    taskId, "TASK_CONFIRM", "工序确认:" + stepType,
                    "SUCCESS", "/api/v1/pda/task/confirm", "POST",
                    httpRequest.getRemoteAddr(), null);
            
            return ApiResponse.success(true);
        } catch (IllegalStateException e) {
            log.warn("工序确认状态非法: taskId={}, stepType={}, error={}", taskId, stepType, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("工序确认失败: taskId={}, stepType={}", taskId, stepType, e);
            return ApiResponse.error(500, "工序确认失败: " + e.getMessage());
        }
    }

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
}
