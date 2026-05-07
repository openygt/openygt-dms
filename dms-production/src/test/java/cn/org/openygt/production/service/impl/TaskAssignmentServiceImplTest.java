package cn.org.openygt.production.service.impl;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.production.entity.TaskAssignment;
import cn.org.openygt.production.mapper.EmployeeSkillMapper;
import cn.org.openygt.production.mapper.HrEmployeeMapper;
import cn.org.openygt.production.mapper.TaskAssignmentMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentServiceImplTest {

    @Mock
    private TaskAssignmentMapper assignmentMapper;
    @Mock
    private EmployeeSkillMapper employeeSkillMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private EquipmentService equipmentService;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private HrEmployeeMapper hrEmployeeMapper;

    @Captor
    private ArgumentCaptor<TaskAssignment> assignmentCaptor;

    private TaskAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskAssignmentServiceImpl(
                assignmentMapper, employeeSkillMapper, taskMapper,
                equipmentService, sysUserMapper, hrEmployeeMapper
        );
    }

    @Nested
    @DisplayName("P0-4: 改派字段保护")
    class ReassignTests {

        @Test
        @DisplayName("正常场景: 只换员工时保留原设备")
        void reassign_onlyEmployee_shouldPreserveDevice() {
            Long assignmentId = 1L;
            TaskAssignment existing = new TaskAssignment();
            existing.setId(assignmentId);
            existing.setTaskId(100L);
            existing.setDeviceId(10L);
            existing.setEmployeeId(20L);
            existing.setStatus(1);

            when(assignmentMapper.selectById(assignmentId)).thenReturn(existing);
            when(assignmentMapper.updateById(any(TaskAssignment.class))).thenReturn(1);

            service.reassign(assignmentId, null, 21L, "员工调休");

            verify(assignmentMapper).updateById(assignmentCaptor.capture());
            TaskAssignment captured = assignmentCaptor.getValue();
            assertThat(captured.getDeviceId()).isEqualTo(10L); // 保留原设备
            assertThat(captured.getEmployeeId()).isEqualTo(21L); // 更新员工
        }

        @Test
        @DisplayName("正常场景: 只换设备时保留原员工")
        void reassign_onlyDevice_shouldPreserveEmployee() {
            Long assignmentId = 1L;
            TaskAssignment existing = new TaskAssignment();
            existing.setId(assignmentId);
            existing.setTaskId(100L);
            existing.setDeviceId(10L);
            existing.setEmployeeId(20L);
            existing.setStatus(1);

            when(assignmentMapper.selectById(assignmentId)).thenReturn(existing);
            when(assignmentMapper.updateById(any(TaskAssignment.class))).thenReturn(1);

            service.reassign(assignmentId, 11L, null, "设备维修");

            verify(assignmentMapper).updateById(assignmentCaptor.capture());
            TaskAssignment captured = assignmentCaptor.getValue();
            assertThat(captured.getDeviceId()).isEqualTo(11L); // 更新设备
            assertThat(captured.getEmployeeId()).isEqualTo(20L); // 保留原员工
        }

        @Test
        @DisplayName("正常场景: 同时换员工和设备")
        void reassign_both_shouldUpdateBoth() {
            Long assignmentId = 1L;
            TaskAssignment existing = new TaskAssignment();
            existing.setId(assignmentId);
            existing.setTaskId(100L);
            existing.setDeviceId(10L);
            existing.setEmployeeId(20L);
            existing.setStatus(1);

            when(assignmentMapper.selectById(assignmentId)).thenReturn(existing);
            when(assignmentMapper.updateById(any(TaskAssignment.class))).thenReturn(1);

            service.reassign(assignmentId, 11L, 21L, "整体调整");

            verify(assignmentMapper).updateById(assignmentCaptor.capture());
            TaskAssignment captured = assignmentCaptor.getValue();
            assertThat(captured.getDeviceId()).isEqualTo(11L);
            assertThat(captured.getEmployeeId()).isEqualTo(21L);
        }
    }
}
