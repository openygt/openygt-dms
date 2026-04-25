package cn.org.openygt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

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
        "V7__add_current_scheme_id.sql"
    };

    private final DataSource dataSource;

    public DatabaseInitConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ensureMigrationTable(conn);
            for (String script : SCRIPTS) {
                if (!hasRun(conn, script)) {
                    log.info("执行迁移脚本: {}", script);
                    ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/" + script));
                    markRun(conn, script);
                } else {
                    log.info("跳过已执行脚本: {}", script);
                }
            }
        }
    }

    private void ensureMigrationTable(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sys_migration ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "script VARCHAR(100) NOT NULL UNIQUE,"
                + "executed_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
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
