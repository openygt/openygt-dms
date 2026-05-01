package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.RollbackApproveRequest;
import cn.org.openygt.production.dto.RollbackRequest;
import cn.org.openygt.production.entity.TaskRollback;
import cn.org.openygt.production.service.TaskRollbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskRollbackController {

    private final TaskRollbackService taskRollbackService;

    @PostMapping("/task/{taskId}/rollback")
    public ApiResponse<TaskRollback> rollback(@PathVariable Long taskId, @Validated @RequestBody RollbackRequest req) {
        return ApiResponse.success(taskRollbackService.initiateRollback(taskId, req.getRollbackTo(), req.getReasonCode(), req.getRemark(), req.getOperatorId()));
    }

    @PostMapping("/rollback/{rollbackId}/approve")
    public ApiResponse<TaskRollback> approve(@PathVariable Long rollbackId, @Validated @RequestBody RollbackApproveRequest req) {
        return ApiResponse.success(taskRollbackService.approveRollback(rollbackId, req.getApproverId(), req.getApprovalStatus(), req.getApprovalComment()));
    }

    @GetMapping("/rollback/list")
    public ApiResponse<List<TaskRollback>> list(@RequestParam(required = false) Long taskId,
                                                  @RequestParam(required = false) Integer approvalStatus) {
        return ApiResponse.success(taskRollbackService.listRollbacks(taskId, approvalStatus));
    }
}
