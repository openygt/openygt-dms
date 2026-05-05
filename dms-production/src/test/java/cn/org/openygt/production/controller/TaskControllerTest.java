package cn.org.openygt.production.controller;

import cn.org.openygt.common.exception.GlobalExceptionHandler;
import cn.org.openygt.production.entity.HandoverDetail;
import cn.org.openygt.production.entity.StepLog;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.service.TaskService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TaskService taskService = new TaskService() {
            @Override public Task getByBarcode(String barcode) {
                if ("BAR2024001".equals(barcode)) {
                    Task task = new Task();
                    task.setId(1L);
                    task.setBarcode("BAR2024001");
                    task.setStatus("待煎药");
                    task.setPrescriptionId(100L);
                    task.setPrescriptionNumber("P2024001");
                    return task;
                }
                return null;
            }
            @Override public Task bindDevice(Long taskId, String deviceCode) { return null; }
            @Override public IPage<Task> queryTasks(String status, Long deviceId, Long id, Long prescriptionId, String operatorId, String operatorKeyword, String prescriptionNumber, String startTime, String endTime, int page, int size) { return null; }
            @Override public Task updateTemperature(String deviceCode, BigDecimal temperature) { return null; }
            @Override public Task startSoak(Long taskId, String operatorId) { return null; }
            @Override public Task endSoak(Long taskId, String operatorId) { return null; }
            @Override public Task startDecoct(Long taskId, String deviceCode, String operatorId) { return null; }
            @Override public Task endDecoct(Long taskId, String operatorId) { return null; }
            @Override public Task startPour(Long taskId, String operatorId) { return null; }
            @Override public Task endPour(Long taskId, String operatorId) { return null; }
            @Override public Task startWrap(Long taskId, String deviceCode, String operatorId) { return null; }
            @Override public Task endWrap(Long taskId, String operatorId) { return null; }
            @Override public Task confirmLabel(Long taskId, String operatorId) { return null; }
            @Override public Task qualityInspect(Long taskId, cn.org.openygt.common.enums.InspectionResultType result, String operatorId, String remark, String reworkNode) { return null; }
            @Override public Task qualityInspectWithItems(Long taskId, cn.org.openygt.common.enums.InspectionResultType result, String operatorId, String remark, String reworkNode, List<cn.org.openygt.common.dto.InspectionItemDTO> items) { return null; }
            @Override public Task handover(Long taskId, Integer bagCount, String handoverType, String handoverUser, String remark, Boolean isFinal) { return null; }
            @Override public StepLog pauseStep(Long stepLogId, String reason) { return null; }
            @Override public StepLog resumeStep(Long stepLogId) { return null; }
            @Override public List<StepLog> queryStepLogs(Long taskId) { return null; }
            @Override public List<HandoverDetail> queryHandoverDetails(Long taskId) { return null; }
            @Override public Task getById(Long taskId) { return null; }
            @Override public int clearAll() { return 0; }
            @Override public List<Task> queryPrintTasks(String printStatus) { return null; }
            @Override public Task printLabel(Long taskId, String deviceCode, String operatorId) { return null; }
            @Override public Task retryPrint(Long taskId, String deviceCode, String operatorId) { return null; }
            @Override public Task forceStatus(Long taskId, String targetStatus, String operatorId, String deviceCode, String remark) { return null; }
            @Override public Task suspendTask(Long taskId, String operatorId, String reason, Integer suspendType) { return null; }
            @Override public Task resumeTask(Long taskId, String operatorId) { return null; }
            @Override public Task assignOperator(Long taskId, String newOperatorId, String newOperatorName, String actingOperatorId) { return null; }
        };
        TaskController controller = new TaskController(taskService, null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getByBarcode_withExistingTask_shouldReturnPrescriptionNumber() throws Exception {
        mockMvc.perform(get("/api/v1/prod/tasks/barcode/BAR2024001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.barcode").value("BAR2024001"))
                .andExpect(jsonPath("$.data.status").value("待煎药"))
                .andExpect(jsonPath("$.data.prescriptionNumber").value("P2024001"));
    }

    @Test
    void getByBarcode_withNonExistingTask_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/prod/tasks/barcode/BAR999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("任务不存在: BAR999"));
    }
}
