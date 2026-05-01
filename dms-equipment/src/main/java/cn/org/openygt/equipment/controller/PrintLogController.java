package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.dto.PrintLogDTO;
import cn.org.openygt.equipment.mapper.PrintLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/print-logs")
@RequiredArgsConstructor
public class PrintLogController {

    private final PrintLogMapper printLogMapper;

    @GetMapping
    public ApiResponse<List<PrintLogDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceCode) {
        return ApiResponse.success(printLogMapper.listPrintLogs(status, deviceCode));
    }
}
