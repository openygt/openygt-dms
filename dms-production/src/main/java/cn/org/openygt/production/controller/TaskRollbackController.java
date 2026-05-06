package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.RollbackApproveRequest;
import cn.org.openygt.production.dto.RollbackRequest;
import cn.org.openygt.production.entity.RollbackReason;
import cn.org.openygt.production.entity.TaskRollback;
import cn.org.openygt.production.service.TaskRollbackService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.org.openygt.production.ProductionModule;

import java.util.List;

@RestController
@RequestMapping(ProductionModule.API_PREFIX)
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
    public ApiResponse<IPage<TaskRollback>> list(@RequestParam(required = false) Long taskId,
                                                   @RequestParam(required = false) Integer approvalStatus,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(taskRollbackService.listRollbacks(taskId, approvalStatus, page, size));
    }

    @GetMapping("/rollback/reasons")
    public ApiResponse<List<RollbackReason>> reasons() {
        return ApiResponse.success(taskRollbackService.listActiveReasons());
    }
}
