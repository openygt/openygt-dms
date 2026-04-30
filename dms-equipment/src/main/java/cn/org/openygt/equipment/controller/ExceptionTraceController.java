package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.mapper.ExceptionTraceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/trace/exceptions")
@RequiredArgsConstructor
public class ExceptionTraceController {

    private final ExceptionTraceMapper exceptionTraceMapper;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String exceptionType,
            @RequestParam(required = false) String handleStatus,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> records = exceptionTraceMapper.list(keyword, exceptionType, handleStatus, dateStart, dateEnd, (page - 1) * size, size);
        long total = exceptionTraceMapper.listCount(keyword, exceptionType, handleStatus, dateStart, dateEnd);
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getById(@PathVariable Long id) {
        return ApiResponse.success(exceptionTraceMapper.getById(id));
    }

    @GetMapping("/stat")
    public ApiResponse<Map<String, Object>> stat(
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd) {
        Map<String, Object> result = new HashMap<>();
        result.put("byType", exceptionTraceMapper.statByType(dateStart, dateEnd));
        result.put("byMonth", exceptionTraceMapper.statByMonth(dateStart, dateEnd));
        return ApiResponse.success(result);
    }
}
