package cn.org.openygt.quality.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.service.QualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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

    /**
     * 执行质检。
     *
     * @param taskId     任务 ID
     * @param result     质检结果：通过 / 让步放行 / 返工 / 报废
     * @param operatorId 操作人
     * @param remark     备注
     * @return 质检结果
     */
    @PostMapping("/inspect")
    public ApiResponse<InspectionResult> inspect(
            @RequestParam @NotNull Long taskId,
            @RequestParam @NotBlank String result,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String remark) {
        return ApiResponse.success(qualityService.inspect(taskId, result, operatorId, remark));
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
}
