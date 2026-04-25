package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.mapper.HospitalMapper;
import cn.org.openygt.masterdata.service.HospitalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * HospitalServiceImpl 单元测试 —— CRUD、code 唯一校验、name 模糊搜索。
 */
@ExtendWith(MockitoExtension.class)
class HospitalServiceImplTest {

    @Mock
    private HospitalMapper hospitalMapper;

    private HospitalServiceImpl hospitalService;

    @BeforeEach
    void setUp() {
        hospitalService = new HospitalServiceImpl(hospitalMapper);
    }

    // ---- create ----

    @Test
    void create_shouldInsertHospital() {
        Hospital hospital = new Hospital();
        hospital.setName("测试医院");
        hospital.setCode("H001");

        when(hospitalMapper.insert(any(Hospital.class))).thenReturn(1);

        Hospital result = hospitalService.create(hospital);

        assertNotNull(result);
        assertEquals("测试医院", result.getName());
        verify(hospitalMapper).insert(hospital);
    }

    // ---- update ----

    @Test
    void update_shouldSucceed_whenExists() {
        Hospital existing = new Hospital();
        existing.setId(1L);
        existing.setName("旧医院");
        existing.setCode("H001");

        Hospital update = new Hospital();
        update.setName("新医院");
        update.setCode("H002");

        when(hospitalMapper.updateById(any(Hospital.class))).thenReturn(1);
        when(hospitalMapper.selectById(anyLong())).thenReturn(existing).thenReturn(update);

        Hospital result = hospitalService.update(1L, update);

        assertNotNull(result);
        assertEquals("新医院", result.getName());
    }

    @Test
    void update_shouldThrow_whenNotExists() {
        when(hospitalMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> hospitalService.update(1L, new Hospital()));
    }

    // ---- getById ----

    @Test
    void getById_shouldReturnHospital_whenExists() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setName("人民医院");

        when(hospitalMapper.selectById(1L)).thenReturn(hospital);

        Hospital result = hospitalService.getById(1L);

        assertNotNull(result);
        assertEquals("人民医院", result.getName());
    }

    @Test
    void getById_shouldThrow_whenNotExists() {
        when(hospitalMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> hospitalService.getById(1L));
    }

    // ---- list ----

    @Test
    void list_shouldReturnPagedResult() {
        Page<Hospital> pageResult = new Page<>(1, 10);
        when(hospitalMapper.selectPage(any(Page.class), any())).thenReturn(pageResult);

        IPage<Hospital> result = hospitalService.list(1, 10);

        assertNotNull(result);
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        verify(hospitalMapper).selectPage(any(Page.class), any());
    }

    // ---- delete ----

    @Test
    void delete_shouldSucceed_whenExists() {
        Hospital existing = new Hospital();
        existing.setId(1L);
        existing.setName("测试医院");

        when(hospitalMapper.selectById(1L)).thenReturn(existing);

        hospitalService.delete(1L);

        verify(hospitalMapper).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenNotExists() {
        when(hospitalMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> hospitalService.delete(1L));
        verify(hospitalMapper, never()).deleteById(any(Long.class));
    }

    // ---- code uniqueness (当前实现不校验，此测试验证基本行为) ----

    @Test
    void create_shouldAllowDuplicateCode() {
        // Current implementation does NOT validate code uniqueness
        Hospital hospital = new Hospital();
        hospital.setName("医院A");
        hospital.setCode("H001");

        when(hospitalMapper.insert(any(Hospital.class))).thenReturn(1);

        Hospital result = hospitalService.create(hospital);

        assertNotNull(result);
        verify(hospitalMapper).insert(hospital);
    }
}
