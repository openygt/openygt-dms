package cn.org.openygt.analytics.service.impl;

import cn.org.openygt.analytics.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> executeArchive(String tableName, String archiveBeforeDate, int batchSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("tableName", tableName);
        result.put("archiveBeforeDate", archiveBeforeDate);

        // 简化实现：实际生产环境应使用分页批量迁移
        try {
            if ("prod_task".equals(tableName)) {
                int count = jdbcTemplate.update(
                    "INSERT INTO prod_task_archive SELECT *, NOW() as archived_at FROM prod_task " +
                    "WHERE complete_time < ? AND deleted = 0 LIMIT ?",
                    archiveBeforeDate, batchSize
                );
                result.put("archivedCount", count);
            } else if ("eq_temperature_log".equals(tableName)) {
                int count = jdbcTemplate.update(
                    "INSERT INTO eq_temperature_log_archive SELECT *, NOW() as archived_at FROM eq_temperature_log " +
                    "WHERE recorded_at < ? LIMIT ?",
                    archiveBeforeDate, batchSize
                );
                result.put("archivedCount", count);
            } else {
                result.put("archivedCount", 0);
                result.put("message", "暂不支持的归档表");
            }
        } catch (Exception e) {
            log.error("归档失败", e);
            result.put("error", e.getMessage());
        }

        result.put("elapsedSeconds", 0);
        return result;
    }
}
