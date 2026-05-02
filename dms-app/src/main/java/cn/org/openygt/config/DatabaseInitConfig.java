package cn.org.openygt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
@Order(Ordered.HIGHEST_PRECEDENCE)
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
            Arrays.sort(resources, (r1, r2) -> compareVersionStrings(r1.getFilename(), r2.getFilename()));

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }

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

    private int compareVersionStrings(String f1, String f2) {
        String v1 = extractVersion(f1);
        String v2 = extractVersion(f2);
        if (v1 == null && v2 == null) return f1.compareTo(f2);
        if (v1 == null) return 1;
        if (v2 == null) return -1;
        String[] p1 = v1.substring(1).split("_");
        String[] p2 = v2.substring(1).split("_");
        int len = Math.min(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            try {
                int cmp = Integer.compare(Integer.parseInt(p1[i]), Integer.parseInt(p2[i]));
                if (cmp != 0) return cmp;
            } catch (NumberFormatException e) {
                int cmp = p1[i].compareTo(p2[i]);
                if (cmp != 0) return cmp;
            }
        }
        return Integer.compare(p1.length, p2.length);
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
            // 预创建 shedlock 表，避免调度任务在迁移完成前触发时找不到表
            stmt.execute("CREATE TABLE IF NOT EXISTS shedlock ("
                + "name VARCHAR(64) NOT NULL,"
                + "lock_until TIMESTAMP(3) NOT NULL,"
                + "locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),"
                + "locked_by VARCHAR(255) NOT NULL,"
                + "PRIMARY KEY (name)"
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
