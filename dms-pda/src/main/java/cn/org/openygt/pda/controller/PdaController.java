package cn.org.openygt.pda.controller;

import cn.org.openygt.common.dto.ApiResponse;
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
import cn.org.openygt.rbac.annotation.RequiresPermissions;
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

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Validated PdaLoginRequest request,
                                                   HttpServletRequest httpRequest) {
        // TODO: 集成用户认证服务校验密码
        Long userId = 1L;
        String token = JwtUtil.generateToken(userId, request.getUserCode(), null, null);

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
    @RequiresPermissions("pda:logout")
    public ApiResponse<Boolean> logout(@RequestAttribute("userId") Long userId) {
        boolean success = loginRecordService.logoutByUserId(userId);
        return ApiResponse.success(success);
    }

    @GetMapping("/task/{barcode}")
    @RequiresPermissions("pda:task:query")
    public ApiResponse<Map<String, Object>> queryTaskByBarcode(@PathVariable String barcode) {
        // TODO: 集成生产模块查询任务
        Map<String, Object> task = new HashMap<>();
        task.put("barcode", barcode);
        task.put("taskId", 1001L);
        task.put("status", "待煎药");
        return ApiResponse.success(task);
    }

    @PostMapping("/task/confirm")
    @RequiresPermissions("pda:task:confirm")
    public ApiResponse<Boolean> confirmTaskStep(@RequestBody @Validated PdaTaskConfirmRequest request,
                                                 @RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("username") String username,
                                                 HttpServletRequest httpRequest) {
        // TODO: 集成生产模块工序确认
        log.info("PDA工序确认: userId={}, taskId={}, stepType={}", userId, request.getTaskId(), request.getStepType());

        operationLogService.logOperation(userId, username, request.getDeviceId(), null,
                request.getTaskId(), "TASK_CONFIRM", "工序确认:" + request.getStepType(),
                "SUCCESS", "/api/v1/pda/task/confirm", "POST",
                httpRequest.getRemoteAddr(), null);

        return ApiResponse.success(true);
    }

    @PostMapping("/photo/upload")
    @RequiresPermissions("pda:photo:upload")
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
    @RequiresPermissions("pda:photo:query")
    public ApiResponse<List<PdaReviewPhoto>> listPhotosByTask(@PathVariable Long taskId) {
        return ApiResponse.success(reviewPhotoService.listByTaskId(taskId));
    }

    @PostMapping("/log/operate")
    @RequiresPermissions("pda:log:operate")
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
    @RequiresPermissions("pda:log:query")
    public ApiResponse<List<PdaOperationLog>> listRecentLogs(@RequestAttribute("userId") Long userId,
                                                              @RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.success(operationLogService.listRecentByUserId(userId, limit));
    }

    @GetMapping("/online/list")
    @RequiresPermissions("pda:login:query")
    public ApiResponse<List<PdaLoginRecord>> listOnlineUsers() {
        return ApiResponse.success(loginRecordService.listAllOnline());
    }
}
