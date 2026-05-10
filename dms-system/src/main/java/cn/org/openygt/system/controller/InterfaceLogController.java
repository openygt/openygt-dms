package cn.org.openygt.system.controller;

import cn.org.openygt.common.annotation.RequiresPermissions;
import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.system.SystemModule;
import cn.org.openygt.system.entity.InterfaceLog;
import cn.org.openygt.system.service.InterfaceLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(SystemModule.API_PREFIX + "/interface-logs")
@RequiredArgsConstructor
public class InterfaceLogController {

    private final InterfaceLogService interfaceLogService;

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    @GetMapping
    public ApiResponse<Page<InterfaceLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long interfaceId,
            @RequestParam(required = false) String result) {
        return ApiResponse.success(interfaceLogService.listLogs(interfaceId, result, page, size));
    }
}
