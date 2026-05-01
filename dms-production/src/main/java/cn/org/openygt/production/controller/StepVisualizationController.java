package cn.org.openygt.production.controller;

import cn.org.openygt.production.ProductionModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.StepDetailDTO;
import cn.org.openygt.production.dto.StepInfoDTO;
import cn.org.openygt.production.service.StepVisualizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ProductionModule.API_PREFIX)
@RequiredArgsConstructor
public class StepVisualizationController {

    private final StepVisualizationService stepVisualizationService;

    @GetMapping("/task/{taskId}/steps")
    public ApiResponse<List<StepInfoDTO>> getTaskSteps(@PathVariable Long taskId) {
        return ApiResponse.success(stepVisualizationService.getTaskSteps(taskId));
    }

    @GetMapping("/task/{taskId}/step-detail")
    public ApiResponse<StepDetailDTO> getStepDetail(@PathVariable Long taskId,
                                                     @RequestParam String stepCode) {
        return ApiResponse.success(stepVisualizationService.getStepDetail(taskId, stepCode));
    }
}
