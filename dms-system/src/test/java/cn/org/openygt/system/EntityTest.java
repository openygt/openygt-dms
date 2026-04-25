package cn.org.openygt.system;

import cn.org.openygt.system.entity.SysConfig;
import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.entity.SysUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统实体覆盖测试 —— 覆盖 Lombok 生成的构造器、getter/setter、toString、equals、hashCode。
 */
class EntityTest {

    @Test
    void sysUser_shouldSupportAllFields() {
        SysUser u = new SysUser();
        u.setId(1L);
        u.setUsername("admin");
        u.setPassword("secret");
        u.setRealName("管理员");
        u.setPhone("13800138000");
        u.setStatus("ACTIVE");

        assertEquals(1L, u.getId().longValue());
        assertEquals("admin", u.getUsername());
        assertEquals("secret", u.getPassword());
        assertEquals("管理员", u.getRealName());
        assertEquals("13800138000", u.getPhone());
        assertEquals("ACTIVE", u.getStatus());

        assertNotNull(u.toString());
        assertNotNull(u.hashCode());
        assertNotEquals(u, new Object());
        assertEquals(u, u);
    }

    @Test
    void sysLog_shouldSupportAllFields() {
        SysLog l = new SysLog();
        l.setId(1L);
        l.setUserId("U001");
        l.setAction("UserController.create");
        l.setModule("system");
        l.setDetail("POST /api/v1/sys/users | 耗时=100ms");
        LocalDateTime now = LocalDateTime.now();
        l.setCreatedAt(now);
        l.setUpdatedAt(now);
        l.setIpAddress("192.168.1.1");

        assertEquals(1L, l.getId().longValue());
        assertEquals("U001", l.getUserId());
        assertEquals("UserController.create", l.getAction());
        assertEquals("system", l.getModule());
        assertEquals("POST /api/v1/sys/users | 耗时=100ms", l.getDetail());
        assertEquals("192.168.1.1", l.getIpAddress());
        assertEquals(now, l.getCreatedAt());
        assertEquals(now, l.getUpdatedAt());

        assertNotNull(l.toString());
    }

    @Test
    void sysConfig_shouldSupportAllFields() {
        SysConfig c = new SysConfig();
        c.setId(1L);
        c.setConfigKey("test.key");
        c.setConfigValue("test-value");
        c.setDescription("测试配置");
        LocalDateTime now = LocalDateTime.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);

        assertEquals(1L, c.getId().longValue());
        assertEquals("test.key", c.getConfigKey());
        assertEquals("test-value", c.getConfigValue());
        assertEquals("测试配置", c.getDescription());
        assertEquals(now, c.getCreatedAt());

        assertNotNull(c.toString());
    }
}
