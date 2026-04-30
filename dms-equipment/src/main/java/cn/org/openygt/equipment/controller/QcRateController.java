package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.mapper.QcRateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/report/qc-rate")
@RequiredArgsConstructor
public class QcRateController {

    private final QcRateMapper qcRateMapper;

    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> trend(
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(defaultValue = "day") String groupBy) {
        String format = "%Y-%m-%d";
        if ("week".equals(groupBy)) format = "%Y-%u";
        else if ("month".equals(groupBy)) format = "%Y-%m";
        return ApiResponse.success(qcRateMapper.trend(dateStart, dateEnd, format));
    }

    @GetMapping("/reason")
    public ApiResponse<List<Map<String, Object>>> reason(
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd) {
        return ApiResponse.success(qcRateMapper.reasonStat(dateStart, dateEnd));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd) {
        return ApiResponse.success(qcRateMapper.summary(dateStart, dateEnd));
    }
}
