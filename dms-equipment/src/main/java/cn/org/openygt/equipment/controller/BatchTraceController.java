package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.DecoctionTrace;
import cn.org.openygt.equipment.entity.DecoctionTraceEvent;
import cn.org.openygt.equipment.mapper.BatchTraceMapper;
import cn.org.openygt.equipment.mapper.DecoctionTraceEventMapper;
import cn.org.openygt.equipment.mapper.DecoctionTraceMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/trace/batch")
@RequiredArgsConstructor
public class BatchTraceController {

    private final BatchTraceMapper batchTraceMapper;
    private final DecoctionTraceMapper decoctionTraceMapper;
    private final DecoctionTraceEventMapper eventMapper;

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> search(
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String prescriptionNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<DecoctionTrace> records = batchTraceMapper.search(batchNo, prescriptionNo, (page - 1) * size, size);
        long total = batchTraceMapper.searchCount(batchNo, prescriptionNo);
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/detail")
    public ApiResponse<Map<String, Object>> detail(@RequestParam String batchNo) {
        List<DecoctionTrace> traces = batchTraceMapper.findByBatchNo(batchNo);
        Map<String, Object> result = new HashMap<>();
        result.put("batchNo", batchNo);
        result.put("traceCount", traces.size());
        result.put("traces", traces);
        return ApiResponse.success(result);
    }

    @GetMapping("/timeline/{prescriptionNo}")
    public ApiResponse<List<DecoctionTraceEvent>> timeline(@PathVariable String prescriptionNo) {
        return ApiResponse.success(eventMapper.findByPrescriptionNo(prescriptionNo));
    }

    @GetMapping("/batch-nos")
    public ApiResponse<List<String>> batchNos() {
        return ApiResponse.success(batchTraceMapper.findBatchNos());
    }
}
