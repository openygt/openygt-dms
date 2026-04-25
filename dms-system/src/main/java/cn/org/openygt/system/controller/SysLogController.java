package cn.org.openygt.system.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.service.SysLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sys/logs")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService logService;

    @GetMapping
    public ApiResponse<IPage<SysLog>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(logService.list(keyword, page, size));
    }
}
