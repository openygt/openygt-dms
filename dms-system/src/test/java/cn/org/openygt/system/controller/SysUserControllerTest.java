package cn.org.openygt.system.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.system.service.SysUserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SysUserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SysUserService userService;

    @InjectMocks
    private SysUserController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnUser() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("newuser");

        when(userService.create(any(SysUser.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/sys/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void update_shouldReturnUser() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setRealName("更新的名字");

        when(userService.update(anyLong(), any(SysUser.class))).thenReturn(user);

        mockMvc.perform(put("/api/v1/sys/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"realName\":\"更新的名字\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("更新的名字"));
    }

    @Test
    void getById_shouldReturnUser() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");

        when(userService.getById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/v1/sys/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void list_shouldReturnPagedResult() throws Exception {
        Page<SysUser> page = new Page<>(1, 10);
        when(userService.list(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/sys/users")
                        .param("keyword", "admin")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/sys/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
