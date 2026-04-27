package cn.org.openygt.print.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.PrintTaskDTO;
import cn.org.openygt.common.service.PrintService;
import cn.org.openygt.print.PrintModule;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(PrintModule.API_PREFIX)
@RequiredArgsConstructor
public class PrintController {

    private final PrintService printService;

    @GetMapping("/tasks")
    public ApiResponse<IPage<PrintTaskDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(printService.getPrintTasks(page, size));
    }

    @PostMapping("/tasks/{taskId}/submit")
    public ApiResponse<Void> submitPrintTask(
            @PathVariable Long taskId,
            @RequestParam String deviceCode,
            @RequestParam String operatorId) {
        printService.submitPrintTask(taskId, deviceCode, operatorId);
        return ApiResponse.success();
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ApiResponse<Void> retryPrint(
            @PathVariable Long taskId,
            @RequestParam String deviceCode,
            @RequestParam String operatorId) {
        printService.retryPrint(taskId, deviceCode, operatorId);
        return ApiResponse.success();
    }

    @GetMapping("/queue")
    public ApiResponse<Object> getPrintQueue() {
        return ApiResponse.success(printService.getPrintQueue());
    }
}
