package cn.org.openygt.production.controller;
import cn.org.openygt.production.ProductionModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.*;
import cn.org.openygt.production.entity.HandoverDetail;
import cn.org.openygt.production.entity.StepLog;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskStatusHistory;
import cn.org.openygt.production.mapper.TaskStatusHistoryMapper;
import cn.org.openygt.production.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskStatusHistoryMapper historyMapper;

    @PostMapping("/{taskId}/bind")
    public ApiResponse<Task> bindDevice(@PathVariable Long taskId, @Validated @RequestBody DeviceBindRequest req) {
        return ApiResponse.success(taskService.bindDevice(taskId, req.getDeviceCode()));
    }

    @GetMapping
    public ApiResponse<IPage<Task>> queryTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(taskService.queryTasks(status, deviceId, page, size));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<Task> getById(@PathVariable Long taskId) {
        Task task = taskService.getById(taskId);
        return task == null ? ApiResponse.error(404, "任务不存在") : ApiResponse.success(task);
    }

    @PostMapping("/{taskId}/soak/start")
    public ApiResponse<Task> startSoak(@PathVariable Long taskId, @Validated @RequestBody SoakRequest req) {
        return ApiResponse.success(taskService.startSoak(taskId, req.getOperatorId()));
    }

    @PostMapping("/{taskId}/soak/end")
    public ApiResponse<Task> endSoak(@PathVariable Long taskId, @Validated @RequestBody StageRequest req) {
        return ApiResponse.success(taskService.endSoak(taskId, req.getOperatorId()));
    }

    @PostMapping("/{taskId}/decoct/start")
    public ApiResponse<Task> startDecoct(@PathVariable Long taskId, @Validated @RequestBody DecoctStartRequest req) {
        return ApiResponse.success(taskService.startDecoct(taskId, req.getDeviceCode(), req.getOperatorId()));
    }

    @PostMapping("/{taskId}/decoct/end")
    public ApiResponse<Task> endDecoct(@PathVariable Long taskId, @Validated @RequestBody StageRequest req) {
        return ApiResponse.success(taskService.endDecoct(taskId, req.getOperatorId()));
    }

    @PostMapping("/{taskId}/pour/start")
    public ApiResponse<Task> startPour(@PathVariable Long taskId, @Validated @RequestBody StageRequest req) {
        return ApiResponse.success(taskService.startPour(taskId, req.getOperatorId()));
    }

    @PostMapping("/{taskId}/pour/end")
    public ApiResponse<Task> endPour(@PathVariable Long taskId, @Validated @RequestBody StageRequest req) {
        return ApiResponse.success(taskService.endPour(taskId, req.getOperatorId()));
    }

    @PostMapping("/{taskId}/wrap/start")
    public ApiResponse<Task> startWrap(@PathVariable Long taskId, @Validated @RequestBody DecoctStartRequest req) {
        return ApiResponse.success(taskService.startWrap(taskId, req.getDeviceCode(), req.getOperatorId()));
    }

    @PostMapping("/{taskId}/wrap/end")
    public ApiResponse<Task> endWrap(@PathVariable Long taskId, @Validated @RequestBody StageRequest req) {
        return ApiResponse.success(taskService.endWrap(taskId, req.getOperatorId()));
    }

    @PostMapping("/{taskId}/label/confirm")
    public ApiResponse<Task> confirmLabel(@PathVariable Long taskId, @Validated @RequestBody StageRequest req) {
        return ApiResponse.success(taskService.confirmLabel(taskId, req.getOperatorId()));
    }

    @PostMapping("/{taskId}/quality")
    public ApiResponse<Task> qualityInspect(@PathVariable Long taskId, @Validated @RequestBody QualityInspectRequest req) {
        return ApiResponse.success(taskService.qualityInspect(taskId, req.getResult(), req.getOperatorId(), req.getRemark()));
    }

    @PostMapping("/{taskId}/handover")
    public ApiResponse<Task> handover(@PathVariable Long taskId, @Validated @RequestBody HandoverRequest req) {
        return ApiResponse.success(taskService.handover(taskId, req.getBagCount(), req.getHandoverType(), req.getHandoverUser(), req.getRemark(), req.getIsFinal()));
    }

    @GetMapping("/{taskId}/steps")
    public ApiResponse<List<StepLog>> queryStepLogs(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.queryStepLogs(taskId));
    }

    @GetMapping("/{taskId}/handover-details")
    public ApiResponse<List<HandoverDetail>> queryHandoverDetails(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.queryHandoverDetails(taskId));
    }

    @GetMapping("/{taskId}/history")
    public ApiResponse<List<TaskStatusHistory>> getHistory(@PathVariable Long taskId) {
        LambdaQueryWrapper<TaskStatusHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskStatusHistory::getTaskId, taskId).orderByDesc(TaskStatusHistory::getOperateTime);
        return ApiResponse.success(historyMapper.selectList(wrapper));
    }

    @PostMapping("/clear")
    public ApiResponse<Integer> clearAll() {
        return ApiResponse.success(taskService.clearAll());
    }

    @GetMapping("/print-queue")
    public ApiResponse<List<Task>> printQueue(@RequestParam(required = false) String printStatus) {
        return ApiResponse.success(taskService.queryPrintTasks(printStatus));
    }

    @PostMapping("/{taskId}/print")
    public ApiResponse<Task> printLabel(@PathVariable Long taskId, @Validated @RequestBody DecoctStartRequest req) {
        return ApiResponse.success(taskService.printLabel(taskId, req.getDeviceCode(), req.getOperatorId()));
    }

    @PostMapping("/{taskId}/print/retry")
    public ApiResponse<Task> retryPrint(@PathVariable Long taskId, @Validated @RequestBody DecoctStartRequest req) {
        return ApiResponse.success(taskService.retryPrint(taskId, req.getDeviceCode(), req.getOperatorId()));
    }

    @PostMapping("/{taskId}/force")
    public ApiResponse<Task> forceStatus(@PathVariable Long taskId, @Validated @RequestBody ForceRequest req) {
        return ApiResponse.success(taskService.forceStatus(taskId, req.getTargetStatus(), req.getOperatorId(), req.getDeviceCode(), req.getRemark()));
    }
}
