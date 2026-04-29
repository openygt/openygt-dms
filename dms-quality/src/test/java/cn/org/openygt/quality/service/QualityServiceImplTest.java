package cn.org.openygt.quality.service;

import cn.org.openygt.common.dto.InspectionResult;
import cn.org.openygt.common.dto.ProdTaskDTO;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.common.service.ProductionQueryService;
import cn.org.openygt.quality.entity.Inspection;
import cn.org.openygt.quality.mapper.InspectionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class QualityServiceImplTest {

    @Mock
    private InspectionMapper inspectionMapper;

    @Mock
    private ProductionQueryService productionQueryService;

    private QualityServiceImpl qualityService;

    @Captor
    private ArgumentCaptor<Inspection> inspectionCaptor;

    @BeforeEach
    void setUp() {
        qualityService = new QualityServiceImpl(inspectionMapper, productionQueryService);
    }

    // ==================== inspect ====================

    @Test
    @DisplayName("质检 PASS：返回 nextStatus=待交接, isException=0")
    void inspect_pass() {
        ProdTaskDTO task = new ProdTaskDTO();
        task.setId(1L);
        task.setStatus("待质检");
        when(productionQueryService.getTaskById(1L)).thenReturn(task);

        InspectionResult result = qualityService.inspect(1L, InspectionResultType.PASS, "QC001", null, null);

        assertThat(result.getNextStatus()).isEqualTo("待交接");
        assertThat(result.getIsException()).isEqualTo(0);
        assertThat(result.getExceptionReason()).isNull();
    }

    @Test
    @DisplayName("质检 CONCESSION：返回 nextStatus=待交接, isException=1, exceptionReason=remark")
    void inspect_concession() {
        ProdTaskDTO task = new ProdTaskDTO();
        task.setId(1L);
        task.setStatus("待质检");
        when(productionQueryService.getTaskById(1L)).thenReturn(task);

        InspectionResult result = qualityService.inspect(1L, InspectionResultType.CONCESSION, "QC001", "颜色偏差", null);

        assertThat(result.getNextStatus()).isEqualTo("待交接");
        assertThat(result.getIsException()).isEqualTo(1);
        assertThat(result.getExceptionReason()).isEqualTo("颜色偏差");
    }

    @Test
    @DisplayName("质检 REWORK：返回 nextStatus=待煎药, isException=1")
    void inspect_rework() {
        ProdTaskDTO task = new ProdTaskDTO();
        task.setId(1L);
        task.setStatus("待质检");
        when(productionQueryService.getTaskById(1L)).thenReturn(task);

        InspectionResult result = qualityService.inspect(1L, InspectionResultType.REWORK, "QC001", "浓度不足", null);

        assertThat(result.getNextStatus()).isEqualTo("待煎药");
        assertThat(result.getIsException()).isEqualTo(1);
        assertThat(result.getExceptionReason()).isEqualTo("浓度不足");
    }

    @Test
    @DisplayName("质检 SCRAP：返回 nextStatus=已报废, isException=1")
    void inspect_scrap() {
        ProdTaskDTO task = new ProdTaskDTO();
        task.setId(1L);
        task.setStatus("待质检");
        when(productionQueryService.getTaskById(1L)).thenReturn(task);

        InspectionResult result = qualityService.inspect(1L, InspectionResultType.SCRAP, "QC001", "污染", null);

        assertThat(result.getNextStatus()).isEqualTo("已报废");
        assertThat(result.getIsException()).isEqualTo(1);
        assertThat(result.getExceptionReason()).isEqualTo("污染");
    }

    @Test
    @DisplayName("质检任务不存在时抛 IllegalArgumentException")
    void inspect_taskNotFound() {
        when(productionQueryService.getTaskById(999L)).thenReturn(null);

        assertThatThrownBy(() -> qualityService.inspect(999L, InspectionResultType.PASS, "QC001", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("任务不存在");
    }

    @Test
    @DisplayName("质检时在 Inspection 实体上正确设置 isException 和 exceptionReason 字段")
    void inspect_shouldSetExceptionFieldsOnEntity() {
        ProdTaskDTO task = new ProdTaskDTO();
        task.setStatus("待质检");
        when(productionQueryService.getTaskById(anyLong())).thenReturn(task);

        qualityService.inspect(1L, InspectionResultType.PASS, "QC001", null, null);
        qualityService.inspect(1L, InspectionResultType.CONCESSION, "QC001", "偏差", null);
        qualityService.inspect(1L, InspectionResultType.REWORK, "QC001", "不足", null);
        qualityService.inspect(1L, InspectionResultType.SCRAP, "QC001", "污染", null);

        verify(inspectionMapper, times(4)).insert(inspectionCaptor.capture());
        List<Inspection> allInspections = inspectionCaptor.getAllValues();

        assertThat(allInspections).hasSize(4);
        // PASS: isException=0
        assertThat(allInspections.get(0).getIsException()).isEqualTo(0);
        assertThat(allInspections.get(0).getExceptionReason()).isNull();
        // CONCESSION: isException=1
        assertThat(allInspections.get(1).getIsException()).isEqualTo(1);
        assertThat(allInspections.get(1).getExceptionReason()).isEqualTo("偏差");
        // REWORK: isException=1
        assertThat(allInspections.get(2).getIsException()).isEqualTo(1);
        assertThat(allInspections.get(2).getExceptionReason()).isEqualTo("不足");
        // SCRAP: isException=1
        assertThat(allInspections.get(3).getIsException()).isEqualTo(1);
        assertThat(allInspections.get(3).getExceptionReason()).isEqualTo("污染");
    }

    // ==================== getInspectionByTaskId ====================

    @Test
    @DisplayName("查询质检记录：有记录时返回结果")
    void getInspectionByTaskId_exists() {
        Inspection inspection = new Inspection();
        inspection.setId(10L);
        inspection.setTaskId(1L);
        inspection.setResult(InspectionResultType.PASS);
        inspection.setOperatorId("QC001");
        inspection.setRemark("合格");
        inspection.setIsException(0);
        inspection.setExceptionReason(null);
        when(inspectionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inspection);

        InspectionResult result = qualityService.getInspectionByTaskId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getInspectionId()).isEqualTo(10L);
        assertThat(result.getTaskId()).isEqualTo(1L);
        assertThat(result.getResult()).isEqualTo(InspectionResultType.PASS);
        assertThat(result.getIsException()).isEqualTo(0);
        assertThat(result.getExceptionReason()).isNull();
    }

    @Test
    @DisplayName("查询质检记录：无记录时返回 null")
    void getInspectionByTaskId_notExists() {
        when(inspectionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        InspectionResult result = qualityService.getInspectionByTaskId(999L);

        assertThat(result).isNull();
    }

    // ==================== inspect: 校验 Inspection 实体写入 ====================

    @Test
    @DisplayName("质检写入 Inspection 实体验证（PASS）")
    void inspect_shouldWriteInspectionEntity_forPass() {
        ProdTaskDTO task = new ProdTaskDTO();
        task.setId(1L);
        task.setStatus("待质检");
        when(productionQueryService.getTaskById(1L)).thenReturn(task);

        qualityService.inspect(1L, InspectionResultType.PASS, "QC001", null, null);

        verify(inspectionMapper).insert(inspectionCaptor.capture());
        Inspection entity = inspectionCaptor.getValue();
        assertThat(entity.getTaskId()).isEqualTo(1L);
        assertThat(entity.getResult()).isEqualTo(InspectionResultType.PASS);
        assertThat(entity.getOperatorId()).isEqualTo("QC001");
        assertThat(entity.getIsException()).isEqualTo(0);
    }

    @Test
    @DisplayName("质检写入 Inspection 实体验证（SCRAP）")
    void inspect_shouldWriteInspectionEntity_forScrap() {
        ProdTaskDTO task = new ProdTaskDTO();
        task.setId(1L);
        task.setStatus("待质检");
        when(productionQueryService.getTaskById(1L)).thenReturn(task);

        qualityService.inspect(1L, InspectionResultType.SCRAP, "QC001", "严重污染", null);

        verify(inspectionMapper).insert(inspectionCaptor.capture());
        Inspection entity = inspectionCaptor.getValue();
        assertThat(entity.getResult()).isEqualTo(InspectionResultType.SCRAP);
        assertThat(entity.getIsException()).isEqualTo(1);
        assertThat(entity.getExceptionReason()).isEqualTo("严重污染");
    }
}
