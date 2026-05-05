package cn.org.openygt.quality.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.dto.InspectionSummaryDTO;
import cn.org.openygt.common.dto.InspectionTrendDTO;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.service.QualityService;
import cn.org.openygt.quality.dto.InspectExecuteRequest;
import cn.org.openygt.quality.entity.Inspection;
import cn.org.openygt.quality.entity.InspectionItem;
import cn.org.openygt.quality.service.QualityServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 质量追溯接口。
 *
 * <p>API 前缀: /api/v1/qt</p>
 */
@RestController
@RequestMapping(QualityController.API_PREFIX)
@RequiredArgsConstructor
@Validated
public class QualityController {

    public static final String API_PREFIX = "/api/v1/qt";

    private final QualityService qualityService;
    private final QualityServiceImpl qualityServiceImpl;

    /**
     * 执行质检（旧版，已废弃）。请使用生产模块的 POST /v1/prod/tasks/{id}/quality。
     */
    @Deprecated
    @PostMapping("/inspect")
    public ApiResponse<InspectionResult> inspect(
            @RequestParam @NotNull Long taskId,
            @RequestParam @NotNull InspectionResultType result,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String reworkNode) {
        return ApiResponse.success(qualityService.inspect(taskId, result, operatorId, remark, reworkNode));
    }

    /**
     * 查询任务最新质检记录。
     *
     * @param taskId 任务 ID
     * @return 质检结果
     */
    @GetMapping("/inspection/{taskId}")
    public ApiResponse<InspectionResult> getInspection(@PathVariable Long taskId) {
        return ApiResponse.success(qualityService.getInspectionByTaskId(taskId));
    }

    /**
     * 质检记录分页列表。
     */
    @GetMapping("/inspections")
    public ApiResponse<Page<Inspection>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResponse.success(qualityServiceImpl.listInspections(result, startTime, endTime, page, size));
    }

    /**
     * 带检查项明细的质检执行（Phase 5.5，内部补录用）。
     * 正常流程请使用生产模块的 POST /v1/prod/tasks/{id}/quality-detail。
     */
    @PostMapping("/inspect/detail")
    public ApiResponse<InspectionResult> inspectWithItems(@Validated @RequestBody InspectExecuteRequest req) {
        return ApiResponse.success(qualityServiceImpl.inspectWithItems(req));
    }

    /**
     * 质检统计汇总。
     */
    @GetMapping("/report/summary")
    public ApiResponse<InspectionSummaryDTO> summary(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LocalDateTime from = parseDateTime(startTime);
        LocalDateTime to = parseDateTime(endTime);
        return ApiResponse.success(qualityServiceImpl.getInspectionSummary(from, to));
    }

    /**
     * 质检趋势统计。
     */
    @GetMapping("/report/trend")
    public ApiResponse<List<InspectionTrendDTO>> trend(
            @RequestParam(required = false, defaultValue = "day") String groupBy) {
        return ApiResponse.success(qualityServiceImpl.getInspectionTrend(groupBy));
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(s.replace(" ", "T"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询质检记录的检查项明细。
     */
    @GetMapping("/inspection/{inspectionId}/items")
    public ApiResponse<List<InspectionItem>> getInspectionItems(@PathVariable Long inspectionId) {
        return ApiResponse.success(qualityServiceImpl.getInspectionItems(inspectionId));
    }
}
