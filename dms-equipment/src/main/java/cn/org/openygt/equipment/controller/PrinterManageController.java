package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.mapper.PrinterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/printers")
@RequiredArgsConstructor
public class PrinterManageController {

    private final PrinterMapper printerMapper;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer deviceType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<EqDevice> records = printerMapper.listPrinters(keyword, deviceType, (page - 1) * size, size);
        long total = printerMapper.listPrintersCount(keyword, deviceType);
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<EqDevice> getById(@PathVariable Long id) {
        return ApiResponse.success(printerMapper.selectById(id));
    }

    @PostMapping("/{id}/test")
    public ApiResponse<String> testPrint(@PathVariable Long id) {
        return ApiResponse.success("测试打印任务已发送");
    }
}
