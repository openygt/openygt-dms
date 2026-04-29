package cn.org.openygt.controller.inventory;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.inventory.InventoryModule;
import cn.org.openygt.inventory.dto.ConsumeRecordDTO;
import cn.org.openygt.inventory.dto.ConsumeRecordRequest;
import cn.org.openygt.inventory.service.ConsumeRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping(InventoryModule.API_PREFIX + "/consume")
@RequiredArgsConstructor
@Validated
public class ConsumeRecordController {

    private final ConsumeRecordService consumeRecordService;

    @PostMapping("/record")
    public ApiResponse<List<Long>> recordConsume(@RequestBody @Validated ConsumeRecordRequest request) {
        List<Long> ids = consumeRecordService.recordConsume(request);
        return ApiResponse.success(ids);
    }

    @GetMapping("/task/{taskId}")
    public ApiResponse<List<ConsumeRecordDTO>> listByTaskId(@PathVariable Long taskId) {
        List<ConsumeRecordDTO> list = consumeRecordService.listByTaskId(taskId);
        return ApiResponse.success(list);
    }

    @GetMapping("/list")
    public ApiResponse<IPage<ConsumeRecordDTO>> pageQuery(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        IPage<ConsumeRecordDTO> result = consumeRecordService.pageQuery(taskId, medicineId, operatorId, startTime, endTime, page, size);
        return ApiResponse.success(result);
    }
}
