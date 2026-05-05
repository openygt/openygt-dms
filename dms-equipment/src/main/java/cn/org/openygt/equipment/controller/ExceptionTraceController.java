package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.ProdExceptionLog;
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
        String normalizedDateEnd = normalizeRangeEnd(dateEnd);
        List<ProdExceptionLog> records = exceptionTraceMapper.list(keyword, exceptionType, handleStatus, dateStart, normalizedDateEnd, (page - 1) * size, size);
        long total = exceptionTraceMapper.listCount(keyword, exceptionType, handleStatus, dateStart, normalizedDateEnd);
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProdExceptionLog> getById(@PathVariable Long id) {
        return ApiResponse.success(exceptionTraceMapper.getById(id));
    }

    @GetMapping("/stat")
    public ApiResponse<Map<String, Object>> stat(
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd) {
        String normalizedDateEnd = normalizeRangeEnd(dateEnd);
        Map<String, Object> result = new HashMap<>();
        result.put("byType", exceptionTraceMapper.statByType(dateStart, normalizedDateEnd));
        result.put("byMonth", exceptionTraceMapper.statByMonth(dateStart, normalizedDateEnd));
        result.put("byStatus", exceptionTraceMapper.statByStatus(dateStart, normalizedDateEnd));
        return ApiResponse.success(result);
    }

    /** 日期区间终点：纯日期则补 23:59:59，避免 le(created_at, '2026-05-05') 漏掉当天数据。 */
    private static String normalizeRangeEnd(String dateEnd) {
        if (dateEnd == null || dateEnd.isEmpty()) {
            return null;
        }
        String t = dateEnd.trim();
        return t.length() == 10 ? t + " 23:59:59" : t;
    }
}
