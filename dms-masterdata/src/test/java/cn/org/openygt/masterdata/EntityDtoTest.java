package cn.org.openygt.masterdata;

import cn.org.openygt.masterdata.dto.HospitalResponse;
import cn.org.openygt.masterdata.dto.SchemeCreateRequest;
import cn.org.openygt.masterdata.dto.SchemeResponse;
import cn.org.openygt.masterdata.dto.SchemeUpdateRequest;
import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.entity.Hospital;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实体和 DTO 覆盖测试 —— 覆盖 Lombok 生成的构造器、getter/setter、toString、equals、hashCode。
 */
class EntityDtoTest {

    @Test
    void decoctScheme_shouldSupportAllFields() {
        DecoctScheme s = new DecoctScheme();
        s.setId(1L);
        s.setCode("S001");
        s.setName("测试方案");
        s.setSchemeType(1);
        s.setDecoctTimes(2);
        s.setPressure(1);
        s.setUpperWater(new BigDecimal("10.5"));
        s.setHeatingTime(30);
        s.setPreHeatingTime(5);
        s.setPostHeatingTime(10);
        s.setDescription("测试描述");
        s.setAlarmHighTemp(new BigDecimal("120"));
        s.setAlarmLowTemp(new BigDecimal("80"));

        assertEquals(1L, s.getId().longValue());
        assertEquals("S001", s.getCode());
        assertEquals("测试方案", s.getName());
        assertEquals(1, s.getSchemeType().intValue());
        assertEquals(2, s.getDecoctTimes().intValue());
        assertEquals(1, s.getPressure().intValue());
        assertEquals(0, new BigDecimal("10.5").compareTo(s.getUpperWater()));
        assertEquals(30, s.getHeatingTime().intValue());
        assertEquals(5, s.getPreHeatingTime().intValue());
        assertEquals(10, s.getPostHeatingTime().intValue());
        assertEquals("测试描述", s.getDescription());
        assertEquals(0, new BigDecimal("120").compareTo(s.getAlarmHighTemp()));
        assertEquals(0, new BigDecimal("80").compareTo(s.getAlarmLowTemp()));

        assertNotNull(s.toString());
        assertNotNull(s.hashCode());
        assertNotEquals(s, new Object());
        assertEquals(s, s);
    }

    @Test
    void hospital_shouldSupportAllFields() {
        Hospital h = new Hospital();
        h.setId(1L);
        h.setName("人民医院");
        h.setCode("H001");
        h.setTenantId("tenant1");
        LocalDateTime now = LocalDateTime.now();
        h.setCreatedAt(now);
        h.setUpdatedAt(now);

        assertEquals(1L, h.getId().longValue());
        assertEquals("人民医院", h.getName());
        assertEquals("H001", h.getCode());
        assertEquals("tenant1", h.getTenantId());
        assertEquals(now, h.getCreatedAt());
        assertEquals(now, h.getUpdatedAt());

        assertNotNull(h.toString());
    }

    @Test
    void schemeCreateRequest_shouldSupportAllFields() {
        SchemeCreateRequest r = new SchemeCreateRequest();
        r.setName("请求方案");
        r.setSchemeType(2);
        r.setDecoctTimes(3);
        r.setPressure(0);
        r.setUpperWater(new BigDecimal("8.0"));
        r.setHeatingTime(25);
        r.setPreHeatingTime(3);
        r.setPostHeatingTime(8);
        r.setDescription("请求描述");

        assertEquals("请求方案", r.getName());
        assertEquals(2, r.getSchemeType().intValue());
        assertEquals(3, r.getDecoctTimes().intValue());
        assertEquals(0, r.getPressure().intValue());
        assertEquals(0, new BigDecimal("8.0").compareTo(r.getUpperWater()));
        assertEquals(25, r.getHeatingTime().intValue());
        assertEquals(3, r.getPreHeatingTime().intValue());
        assertEquals(8, r.getPostHeatingTime().intValue());
        assertEquals("请求描述", r.getDescription());

        assertNotNull(r.toString());
    }

    @Test
    void schemeUpdateRequest_shouldSupportAllFields() {
        SchemeUpdateRequest r = new SchemeUpdateRequest();
        r.setName("更新请求");
        r.setSchemeType(1);
        r.setDecoctTimes(1);

        assertEquals("更新请求", r.getName());
        assertEquals(1, r.getSchemeType().intValue());
        assertEquals(1, r.getDecoctTimes().intValue());
        assertNotNull(r.toString());
    }

    @Test
    void schemeResponse_shouldSupportAllFields() {
        SchemeResponse r = new SchemeResponse();
        r.setId(1L);
        r.setSchemeName("响应");
        r.setSchemeType(1);
        r.setDecoctTimes(2);
        r.setPressure(1);
        r.setUpperWater(new BigDecimal("5.0"));
        r.setDecoctTime(20);
        r.setSoakTime(2);
        r.setPostHeatingTime(5);
        r.setRemark("响应描述");
        LocalDateTime now = LocalDateTime.now();
        r.setCreatedAt(now);
        r.setUpdatedAt(now);

        assertEquals(1L, r.getId().longValue());
        assertEquals("响应", r.getSchemeName());
        assertEquals(1, r.getSchemeType().intValue());
        assertEquals(2, r.getDecoctTimes().intValue());
        assertEquals(1, r.getPressure().intValue());
        assertEquals(20, r.getDecoctTime().intValue());
        assertEquals(now, r.getCreatedAt());
        assertNotNull(r.toString());
        assertNotNull(r.hashCode());
    }

    @Test
    void hospitalResponse_shouldSupportAllFields() {
        HospitalResponse r = new HospitalResponse();
        r.setId(1L);
        r.setName("医院响应");
        r.setCode("H999");
        r.setContactPerson("联系人");
        r.setPhone("13800138000");
        r.setAddress("测试地址");
        r.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        r.setCreatedAt(now);
        r.setUpdatedAt(now);

        assertEquals(1L, r.getId().longValue());
        assertEquals("医院响应", r.getName());
        assertEquals("H999", r.getCode());
        assertNotNull(r.toString());
    }
}
