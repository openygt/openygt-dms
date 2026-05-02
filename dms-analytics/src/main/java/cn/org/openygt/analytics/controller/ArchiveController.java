package cn.org.openygt.analytics.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.analytics.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sys/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @PostMapping("/execute")
    public ApiResponse<Map<String, Object>> execute(@RequestBody ArchiveRequest request) {
        return ApiResponse.success(archiveService.executeArchive(
                request.getTableName(), request.getArchiveBeforeDate(), request.getBatchSize()));
    }

    @lombok.Data
    public static class ArchiveRequest {
        private String tableName;
        private String archiveBeforeDate;
        private Integer batchSize = 1000;
    }
}
