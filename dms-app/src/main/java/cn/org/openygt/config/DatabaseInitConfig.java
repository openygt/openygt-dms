package cn.org.openygt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;

@Component
public class DatabaseInitConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitConfig.class);

    private static final String[] SCRIPTS = {
        "V1__init.sql",
        "V2__refactor.sql",
        "V3__new_flow.sql",
        "V4__module_split.sql",
        "V5__v1_4_refactor.sql",
        "V6__equipment_heartbeat.sql",
        "V7__add_current_scheme_id.sql",
        "V8__add_trigger_source.sql",
        "V9__exception_tables.sql",
        "V10__add_user_role.sql",
        "V11__device_group.sql",
        "V12__add_scheme_code.sql",
        "V13__pda_tables.sql",
        "V14__rbac_tables.sql",
        "V15__fix_missing_tables_and_columns.sql",
        "V16__fix_qt_inspection_columns.sql"
    };

    private final DataSource dataSource;

    public DatabaseInitConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            boolean mysql = isMysql(conn);
            ensureMigrationTable(conn, mysql);
            for (String script : SCRIPTS) {
                if (!hasRun(conn, script)) {
                    log.info("执行迁移脚本: {}", script);
                    executeSqlScript(conn, script, mysql);
                    markRun(conn, script);
                } else {
                    log.info("跳过已执行脚本: {}", script);
                }
            }
        }
    }

    private boolean isMysql(Connection conn) throws Exception {
        DatabaseMetaData metaData = conn.getMetaData();
        String productName = metaData.getDatabaseProductName();
        return productName != null && productName.toLowerCase().contains("mysql");
    }

    private void ensureMigrationTable(Connection conn, boolean mysql) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            if (mysql) {
                stmt.execute("CREATE TABLE IF NOT EXISTS sys_migration ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "script VARCHAR(100) NOT NULL UNIQUE,"
                    + "executed_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ")");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS sys_migration ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "script VARCHAR(100) NOT NULL UNIQUE,"
                    + "executed_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            }
        }
    }

    private void executeSqlScript(Connection conn, String script, boolean mysql) throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/" + script);
        if (!mysql) {
            ScriptUtils.executeSqlScript(conn, resource);
            return;
        }

        byte[] sqlBytes = StreamUtils.copyToByteArray(resource.getInputStream());
        String sql = new String(sqlBytes, StandardCharsets.UTF_8);
        String mysqlSql = adaptSqliteSqlToMysql(sql);
        EncodedResource encodedResource = new EncodedResource(
            new ByteArrayResource(mysqlSql.getBytes(StandardCharsets.UTF_8)));
        ScriptUtils.executeSqlScript(
            conn,
            encodedResource,
            true,
            true,
            ScriptUtils.DEFAULT_COMMENT_PREFIX,
            ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER
        );
    }

    private String adaptSqliteSqlToMysql(String sql) {
        String transformed = sql;
        transformed = transformed.replaceAll("(?i)INTEGER\\s+PRIMARY\\s+KEY\\s+AUTOINCREMENT", "BIGINT AUTO_INCREMENT PRIMARY KEY");
        transformed = transformed.replaceAll("(?i)AUTOINCREMENT", "AUTO_INCREMENT");
        transformed = transformed.replaceAll("(?i)\\bINTEGER\\b", "BIGINT");
        transformed = transformed.replaceAll("(?i)INSERT\\s+OR\\s+REPLACE\\s+INTO", "REPLACE INTO");
        transformed = transformed.replaceAll("(?i)INSERT\\s+OR\\s+IGNORE\\s+INTO", "INSERT IGNORE INTO");
        transformed = transformed.replaceAll("(?i)CREATE\\s+INDEX\\s+IF\\s+NOT\\s+EXISTS", "CREATE INDEX");
        transformed = transformed.replace("datetime('now')", "CURRENT_TIMESTAMP");
        return transformed;
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
