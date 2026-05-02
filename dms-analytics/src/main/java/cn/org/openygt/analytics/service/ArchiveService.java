package cn.org.openygt.analytics.service;

import java.util.Map;

public interface ArchiveService {

    Map<String, Object> executeArchive(String tableName, String archiveBeforeDate, int batchSize);
}
