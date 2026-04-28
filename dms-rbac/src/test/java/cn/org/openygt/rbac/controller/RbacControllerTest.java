package cn.org.openygt.rbac.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.rbac.entity.SysMenu;
import cn.org.openygt.rbac.entity.SysRole;
import cn.org.openygt.rbac.service.SysMenuService;
import cn.org.openygt.rbac.service.SysRoleMenuService;
import cn.org.openygt.rbac.service.SysRoleService;
import cn.org.openygt.rbac.service.SysUserRoleService;
import cn.org.openygt.system.service.SysUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RbacControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SysMenuService menuService;
    @Mock
    private SysRoleService roleService;
    @Mock
    private SysUserRoleService userRoleService;
    @Mock
    private SysRoleMenuService roleMenuService;
    @Mock
    private SysUserService userService;

    @InjectMocks
    private RbacController rbacController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rbacController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void menuTree_shouldReturnMenus() throws Exception {
        SysMenu menu = new SysMenu();
        menu.setName("系统管理");
        when(menuService.getMenuTree()).thenReturn(Collections.singletonList(menu));

        mockMvc.perform(get("/api/v1/rbac/menus/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("系统管理"));
    }

    @Test
    void roleList_shouldReturnRoles() throws Exception {
        SysRole role = new SysRole();
        role.setRoleCode("ROLE_ADMIN");
        role.setRoleName("系统管理员");
        when(roleService.list()).thenReturn(Collections.singletonList(role));

        mockMvc.perform(get("/api/v1/rbac/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].roleCode").value("ROLE_ADMIN"));
    }
}
