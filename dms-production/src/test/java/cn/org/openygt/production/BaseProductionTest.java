package cn.org.openygt.production;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * dms-production 集成测试基类。
 *
 * <p>用法：需要数据库交互的集成测试继承此类，自动获得：
 * <ul>
 *   <li>H2 内存数据库（MySQL 兼容模式）</li>
 *   <li>通过 JdbcTemplate 直接操作数据库进行数据准备和断言</li>
 *   <li>MyBatis-Plus 映射器可通过 @Autowired 注入</li>
 * </ul>
 *
 * <p>注意：此类不 Mock 任何 Bean，适合需要验证数据库交互的集成测试。
 * 纯单元测试（Mock 所有 Mapper）应直接使用 {@code @ExtendWith(MockitoExtension.class)}。
 */
@SpringBootTest(classes = TestProductionApplication.class)
@ActiveProfiles("test")
public abstract class BaseProductionTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * 每个测试方法执行前清空所有 prod_ 表，保证测试隔离。
     */
    @BeforeEach
    protected void setUp() {
        cleanupTables();
    }

    private void cleanupTables() {
        jdbcTemplate.execute("DELETE FROM prod_handover_detail");
        jdbcTemplate.execute("DELETE FROM prod_work_record");
        jdbcTemplate.execute("DELETE FROM prod_step_log");
        jdbcTemplate.execute("DELETE FROM prod_task_status_history");
        jdbcTemplate.execute("DELETE FROM prod_task");
        jdbcTemplate.execute("DELETE FROM prod_prescription");
    }
}
