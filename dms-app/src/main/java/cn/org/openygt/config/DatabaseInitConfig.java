package cn.org.openygt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 数据库初始化与迁移配置（MySQL 专用）。
 *
 * <p>自动扫描 classpath:db/migration 下所有 V*.sql 脚本，按文件名排序后执行。</p>
 * <p>重复版本号会被去重（取排序后的第一个）。</p>
 */
@Component
public class DatabaseInitConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitConfig.class);
    private static final String MIGRATION_PATTERN = "classpath:db/migration/V*.sql";

    private final DataSource dataSource;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public DatabaseInitConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ensureMigrationTable(conn);

            Resource[] resources = resolver.getResources(MIGRATION_PATTERN);
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

            String lastExecutedVersion = null;
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                String version = extractVersion(filename);
                if (version != null && version.equals(lastExecutedVersion)) {
                    log.warn("检测到重复版本号 {}，跳过脚本: {}", version, filename);
                    continue;
                }
                lastExecutedVersion = version;

                if (!hasRun(conn, filename)) {
                    log.info("执行迁移脚本: {}", filename);
                    ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/" + filename));
                    markRun(conn, filename);
                } else {
                    log.info("跳过已执行脚本: {}", filename);
                }
            }
        }
    }

    private String extractVersion(String filename) {
        if (filename.startsWith("V") && filename.contains("__")) {
            return filename.substring(0, filename.indexOf("__"));
        }
        return null;
    }

    private void ensureMigrationTable(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sys_migration ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "script VARCHAR(100) NOT NULL UNIQUE,"
                + "executed_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    private boolean hasRun(Connection conn, String script) throws Exception {
        String sql = "SELECT COUNT(*) FROM sys_migration WHERE script = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, script);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void markRun(Connection conn, String script) throws Exception {
        String sql = "INSERT INTO sys_migration (script) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, script);
            stmt.executeUpdate();
        }
    }
}
