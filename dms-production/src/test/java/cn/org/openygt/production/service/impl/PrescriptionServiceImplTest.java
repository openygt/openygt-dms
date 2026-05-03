package cn.org.openygt.production.service.impl;

import cn.org.openygt.equipment.entity.Medicine;
import cn.org.openygt.equipment.mapper.MedicineMapper;
import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.mapper.HospitalMapper;
import cn.org.openygt.masterdata.service.ToxicMedicineService;
import cn.org.openygt.production.config.PrescriptionConfig;
import cn.org.openygt.production.dto.*;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.entity.PrescriptionMedicine;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.mapper.PrescriptionMedicineMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PrescriptionServiceImplTest {

    @Mock
    private PrescriptionMapper prescriptionMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private PrescriptionMedicineMapper prescriptionMedicineMapper;
    @Mock
    private ToxicMedicineService toxicMedicineService;
    @Mock
    private MedicineMapper medicineMapper;
    @Mock
    private HospitalMapper hospitalMapper;
    @Mock
    private PrescriptionConfig prescriptionConfig;

    private PrescriptionServiceImpl service;

    @Captor
    private ArgumentCaptor<Prescription> prescriptionCaptor;
    @Captor
    private ArgumentCaptor<Task> taskCaptor;

    // Mock the toxic medicine service fluent chain (lenient — some methods may not be called per test)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockToxicServiceEmpty() {
        LambdaQueryChainWrapper chain = mock(LambdaQueryChainWrapper.class);
        lenient().when(toxicMedicineService.lambdaQuery()).thenReturn(chain);
        lenient().when(chain.eq(any(), any())).thenReturn(chain);
        lenient().when(chain.in(any(), anyCollection())).thenReturn(chain);
        lenient().when(chain.oneOpt()).thenReturn(Optional.empty());
        lenient().when(chain.list()).thenReturn(Collections.emptyList());
    }

    @BeforeEach
    void setUp() {
        service = new PrescriptionServiceImpl(
                prescriptionMapper, taskMapper, prescriptionMedicineMapper,
                toxicMedicineService, medicineMapper, hospitalMapper,
                prescriptionConfig
        );
    }

    private PrescriptionMedicineItemRequest medItem(String name, BigDecimal dosage, String unit, Long medicineId) {
        PrescriptionMedicineItemRequest item = new PrescriptionMedicineItemRequest();
        item.setMedicineName(name);
        item.setDosage(dosage);
        item.setUnit(unit);
        item.setMedicineId(medicineId);
        return item;
    }

    private PrescriptionStructuredCreateRequest createReq(String patientName, List<PrescriptionMedicineItemRequest> items) {
        PrescriptionStructuredCreateRequest req = new PrescriptionStructuredCreateRequest();
        req.setPatientName(patientName);
        req.setRepetition(3);
        req.setMedicineItems(items);
        return req;
    }

    // ==================== createStructured ====================

    @Nested
    @DisplayName("createStructured: 结构化创建处方")
    class CreateStructured {

        @Test
        @DisplayName("FLEXIBLE模式：正常创建，插入处方+药材+任务")
        void testCreateSuccess() {
            when(prescriptionConfig.isStrictMode()).thenReturn(false);
            when(prescriptionMapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
                Prescription p = invocation.getArgument(0);
                p.setId(100L);
                return 1;
            });
            when(prescriptionMedicineMapper.selectByPrescriptionId(100L))
                    .thenReturn(Collections.emptyList());

            List<PrescriptionMedicineItemRequest> items = Arrays.asList(
                    medItem("当归", new BigDecimal("15"), "g", null),
                    medItem("川芎", new BigDecimal("10"), "g", null)
            );
            PrescriptionStructuredCreateRequest req = createReq("测试患者A", items);

            Prescription result = service.createStructured(req);

            assertEquals("测试患者A", result.getPatientName());
            assertEquals("PENDING", result.getReceiveStatus());
            verify(prescriptionMapper).insert(prescriptionCaptor.capture());
            assertEquals("测试患者A", prescriptionCaptor.getValue().getPatientName());
            verify(prescriptionMedicineMapper, times(2)).insert(any(PrescriptionMedicine.class));
            verify(taskMapper).insert(taskCaptor.capture());
            assertEquals("待泡药", taskCaptor.getValue().getStatus());
            assertEquals(100L, taskCaptor.getValue().getPrescriptionId());
        }

        @Test
        @DisplayName("STRICT模式：medicineId为空时抛出异常")
        void testStrictModeRequiresMedicineId() {
            when(prescriptionConfig.isStrictMode()).thenReturn(true);

            List<PrescriptionMedicineItemRequest> items = Collections.singletonList(
                    medItem("当归", new BigDecimal("15"), "g", null)
            );
            PrescriptionStructuredCreateRequest req = createReq("测试患者B", items);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createStructured(req));
            assertTrue(ex.getMessage().contains("STRICT模式"));
            verify(prescriptionMapper, never()).insert(any(Prescription.class));
        }

        @Test
        @DisplayName("STRICT模式：medicineId不存在时抛出异常")
        void testStrictModeInvalidMedicineId() {
            when(prescriptionConfig.isStrictMode()).thenReturn(true);
            when(medicineMapper.selectById(9999L)).thenReturn(null);

            List<PrescriptionMedicineItemRequest> items = Collections.singletonList(
                    medItem("当归", new BigDecimal("15"), "g", 9999L)
            );
            PrescriptionStructuredCreateRequest req = createReq("测试患者C", items);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createStructured(req));
            assertTrue(ex.getMessage().contains("不存在于系统药品目录"));
        }

        @Test
        @DisplayName("带medicineId时调用毒性校验（无毒性配置则通过）")
        void testCreateWithMedicineIdSkipsToxicCheck() {
            when(prescriptionConfig.isStrictMode()).thenReturn(false);
            mockToxicServiceEmpty();
            when(prescriptionMapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
                Prescription p = invocation.getArgument(0);
                p.setId(101L);
                return 1;
            });
            when(prescriptionMedicineMapper.selectByPrescriptionId(101L))
                    .thenReturn(Collections.emptyList());

            List<PrescriptionMedicineItemRequest> items = Collections.singletonList(
                    medItem("当归", new BigDecimal("15"), "g", 1L)
            );
            PrescriptionStructuredCreateRequest req = createReq("测试患者D", items);

            Prescription result = service.createStructured(req);
            assertEquals("测试患者D", result.getPatientName());
            verify(toxicMedicineService, atLeastOnce()).lambdaQuery();
            verify(prescriptionMapper).insert(any(Prescription.class));
        }
    }

    // ==================== importFromCsv ====================

    @Nested
    @DisplayName("importFromCsv: CSV导入")
    class ImportFromCsv {

        @Test
        @DisplayName("标准CSV导入：按患者分组创建处方")
        void testCsvImport() {
            when(prescriptionConfig.isStrictMode()).thenReturn(false);
            when(prescriptionMapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
                Prescription p = invocation.getArgument(0);
                p.setId(new Random().nextLong());
                return 1;
            });
            when(prescriptionMedicineMapper.selectByPrescriptionId(anyLong()))
                    .thenReturn(Collections.emptyList());

            String csv = "患者姓名,药材名称,用量,单位,用法,付数\n"
                    + "测试CSV-张三,当归,15,g,先煎,3\n"
                    + "测试CSV-张三,川芎,10,g,,3\n"
                    + "测试CSV-李四,金银花,20,g,后下,2\n";

            List<Prescription> results = service.importFromCsv(csv, null, 1);

            assertEquals(2, results.size());
            verify(prescriptionMapper, times(2)).insert(any(Prescription.class));
            // 张三2味药 + 李四1味药 = 3条药材
            verify(prescriptionMedicineMapper, times(3)).insert(any(PrescriptionMedicine.class));
            verify(taskMapper, times(2)).insert(any(Task.class));
        }

        @Test
        @DisplayName("CSV导入：同批次患者+药材完全重复时跳过")
        void testCsvBatchDedup() {
            when(prescriptionConfig.isStrictMode()).thenReturn(false);
            when(prescriptionMapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
                Prescription p = invocation.getArgument(0);
                p.setId(new Random().nextLong());
                return 1;
            });
            when(prescriptionMedicineMapper.selectByPrescriptionId(anyLong()))
                    .thenReturn(Collections.emptyList());

            // 两组完全相同的患者+药材组合
            String csv = "患者姓名,药材名称,用量,单位\n"
                    + "重复患者,当归,15,g\n"
                    + "重复患者,当归,15,g\n";

            List<Prescription> results = service.importFromCsv(csv, null, 1);

            assertEquals(1, results.size());
            verify(prescriptionMapper, times(1)).insert(any(Prescription.class));
        }

        @Test
        @DisplayName("CSV导入：空文件或只有表头时抛出异常")
        void testCsvEmptyOrHeaderOnly() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.importFromCsv("患者姓名,药材名称", null, 1));
        }
    }

    // ==================== createFromPush ====================

    @Nested
    @DisplayName("createFromPush: HIS推送接收")
    class CreateFromPush {

        private PrescriptionPushRequest.PushPrescription pushPrescription(
                String prescriptionNo, String patientName, String medicineCode, String medicineName) {
            PrescriptionPushRequest.PushPrescription push = new PrescriptionPushRequest.PushPrescription();
            push.setPrescriptionNo(prescriptionNo);
            push.setPatientName(patientName);
            push.setRepetition(7);

            PrescriptionPushRequest.PushMedicineItem item = new PrescriptionPushRequest.PushMedicineItem();
            item.setHisMedicineCode(medicineCode);
            item.setHisMedicineName(medicineName);
            item.setDosage("15");
            item.setUnit("g");
            push.setItems(Collections.singletonList(item));
            return push;
        }

        @Test
        @DisplayName("正常推送：创建处方+药材+任务")
        void testPushNormal() {
            when(prescriptionConfig.isStrictMode()).thenReturn(false);
            Hospital hospital = new Hospital();
            hospital.setId(1L);
            when(hospitalMapper.selectOne(any(Wrapper.class))).thenReturn(hospital);
            when(prescriptionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
            when(prescriptionMapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
                Prescription p = invocation.getArgument(0);
                p.setId(200L);
                return 1;
            });
            when(prescriptionMedicineMapper.selectByPrescriptionId(200L))
                    .thenReturn(Collections.emptyList());

            PrescriptionPushRequest.PushPrescription push = pushPrescription("HIS001", "推送患者", "T001", "当归");
            Prescription result = service.createFromPush(push, "TEST_HOSPITAL");

            assertEquals("推送患者", result.getPatientName());
            assertEquals("HIS001", result.getPrescriptionNumber());
            assertEquals("PENDING", result.getReceiveStatus());
            verify(prescriptionMapper).insert(any(Prescription.class));
            verify(prescriptionMedicineMapper).insert(any(PrescriptionMedicine.class));
            verify(taskMapper).insert(any(Task.class));
        }

        @Test
        @DisplayName("幂等检查：同一医院+处方号已存在时返回已有记录")
        void testPushIdempotent() {
            Prescription existing = new Prescription();
            existing.setId(300L);
            existing.setPatientName("已存在患者");
            existing.setPrescriptionNumber("HIS001");
            existing.setReceiveStatus("PENDING");

            when(hospitalMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
                Hospital h = new Hospital();
                h.setId(1L);
                return h;
            });
            when(prescriptionMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
            when(prescriptionMedicineMapper.selectByPrescriptionId(300L))
                    .thenReturn(Collections.emptyList());

            PrescriptionPushRequest.PushPrescription push = pushPrescription("HIS001", "已存在患者", "T001", "当归");
            Prescription result = service.createFromPush(push, "TEST_HOSPITAL");

            assertEquals(300L, result.getId());
            assertEquals("已存在患者", result.getPatientName());
            verify(prescriptionMapper, never()).insert(any(Prescription.class));
            verify(taskMapper, never()).insert(any(Task.class));
        }

        @Test
        @DisplayName("STRICT模式：药品匹配失败时创建异常处方")
        void testPushExceptionOnStrictModeMismatch() {
            when(prescriptionConfig.isStrictMode()).thenReturn(true);
            Hospital hospital = new Hospital();
            hospital.setId(1L);
            when(hospitalMapper.selectOne(any(Wrapper.class))).thenReturn(hospital);
            when(prescriptionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
            when(prescriptionMapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
                Prescription p = invocation.getArgument(0);
                p.setId(400L);
                return 1;
            });
            when(prescriptionMedicineMapper.selectByPrescriptionId(400L))
                    .thenReturn(Collections.emptyList());

            PrescriptionPushRequest.PushPrescription push = pushPrescription("HIS002", "异常患者", null, "未知药材");
            Prescription result = service.createFromPush(push, "TEST_HOSPITAL");

            assertEquals(Integer.valueOf(1), result.getImportException());
            assertNotNull(result.getExceptionReason());
            assertTrue(result.getExceptionReason().contains("缺少编码"));
            verify(taskMapper, never()).insert(any(Task.class));
        }
    }

    // ==================== createFromOcr ====================

    @Nested
    @DisplayName("createFromOcr: OCR确认创建")
    class CreateFromOcr {

        @Test
        @DisplayName("FLEXIBLE模式：尝试匹配药品目录，创建处方")
        void testOcrWithMedicineMatching() {
            when(prescriptionConfig.isStrictMode()).thenReturn(false);
            mockToxicServiceEmpty();

            Medicine matched = new Medicine();
            matched.setId(10L);
            when(medicineMapper.selectOne(any(Wrapper.class))).thenReturn(matched);
            when(prescriptionMapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
                Prescription p = invocation.getArgument(0);
                p.setId(500L);
                return 1;
            });
            when(prescriptionMedicineMapper.selectByPrescriptionId(500L))
                    .thenReturn(Collections.emptyList());

            OcrPrescriptionRequest request = new OcrPrescriptionRequest();
            request.setPatientName("OCR患者");
            request.setRepetition(3);
            OcrPrescriptionRequest.OcrMedicineItem ocrItem = new OcrPrescriptionRequest.OcrMedicineItem();
            ocrItem.setMedicineName("当归");
            ocrItem.setDosage("15");
            ocrItem.setUnit("g");
            request.setItems(Collections.singletonList(ocrItem));

            Prescription result = service.createFromOcr(request);

            assertEquals("OCR患者", result.getPatientName());
            verify(prescriptionMapper).insert(any(Prescription.class));
            verify(prescriptionMedicineMapper).insert(any(PrescriptionMedicine.class));
            verify(taskMapper).insert(any(Task.class));
        }
    }

    // ==================== resolveException ====================

    @Nested
    @DisplayName("resolveException: 异常处方纠正")
    class ResolveException {

        @Test
        @DisplayName("纠正异常处方：更新信息、删除旧药材、创建新药材和任务")
        void testResolveExceptionSuccess() {
            mockToxicServiceEmpty();

            Prescription exceptionPrescription = new Prescription();
            exceptionPrescription.setId(600L);
            exceptionPrescription.setPatientName("异常患者");
            exceptionPrescription.setImportException(1);
            exceptionPrescription.setExceptionReason("药品未匹配");

            when(prescriptionMapper.selectById(600L)).thenReturn(exceptionPrescription);
            when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
            when(prescriptionMedicineMapper.selectByPrescriptionId(600L))
                    .thenReturn(Collections.emptyList());

            PrescriptionStructuredCreateRequest corrected = new PrescriptionStructuredCreateRequest();
            corrected.setPatientName("已纠正患者");
            corrected.setRepetition(5);
            corrected.setMedicineItems(Collections.singletonList(
                    medItem("当归", new BigDecimal("15"), "g", 1L)
            ));

            service.resolveException(600L, corrected);

            assertEquals(Integer.valueOf(0), exceptionPrescription.getImportException());
            assertNull(exceptionPrescription.getExceptionReason());
            assertNull(exceptionPrescription.getRawImportData());
            verify(prescriptionMapper).updateById(exceptionPrescription);
            verify(prescriptionMedicineMapper).delete(any(Wrapper.class));
            verify(prescriptionMedicineMapper).insert(any(PrescriptionMedicine.class));
            verify(taskMapper).insert(any(Task.class));
        }

        @Test
        @DisplayName("非异常处方纠正时抛出异常")
        void testResolveNonExceptionPrescription() {
            Prescription normal = new Prescription();
            normal.setId(700L);
            normal.setImportException(0);
            when(prescriptionMapper.selectById(700L)).thenReturn(normal);

            assertThrows(IllegalArgumentException.class,
                    () -> service.resolveException(700L, new PrescriptionStructuredCreateRequest()));
        }

        @Test
        @DisplayName("不存在处方纠正时抛出异常")
        void testResolveNonExistentPrescription() {
            when(prescriptionMapper.selectById(999L)).thenReturn(null);
            assertThrows(IllegalArgumentException.class,
                    () -> service.resolveException(999L, new PrescriptionStructuredCreateRequest()));
        }
    }

    // ==================== receive & reject ====================

    @Nested
    @DisplayName("receive/reject: 处方接收与驳回")
    class ReceiveReject {

        @Test
        @DisplayName("接收处方：状态流转+任务号生成+无任务时创建")
        void testReceive() {
            Prescription p = new Prescription();
            p.setId(800L);
            p.setPatientName("接收患者");
            p.setReceiveStatus("PENDING");
            when(prescriptionMapper.selectById(800L)).thenReturn(p);
            when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

            service.receive(800L, 1L, "操作员A");

            assertEquals("RECEIVED", p.getReceiveStatus());
            assertNotNull(p.getTaskNo());
            assertTrue(p.getTaskNo().startsWith("D"));
            verify(prescriptionMapper).updateById(p);
            verify(taskMapper).insert(any(Task.class));
        }

        @Test
        @DisplayName("接收非待接收状态处方时抛出异常")
        void testReceiveWrongStatus() {
            Prescription p = new Prescription();
            p.setId(801L);
            p.setReceiveStatus("RECEIVED");
            when(prescriptionMapper.selectById(801L)).thenReturn(p);

            assertThrows(IllegalArgumentException.class,
                    () -> service.receive(801L, 1L, "操作员A"));
        }

        @Test
        @DisplayName("驳回处方：状态流转+原因记录")
        void testReject() {
            Prescription p = new Prescription();
            p.setId(900L);
            p.setPatientName("驳回患者");
            p.setReceiveStatus("PENDING");
            when(prescriptionMapper.selectById(900L)).thenReturn(p);

            service.reject(900L, "MANUAL", "处方有误", 1L, "操作员B");

            assertEquals("REJECTED", p.getReceiveStatus());
            assertEquals("MANUAL", p.getRejectType());
            assertEquals("处方有误", p.getRejectReason());
            verify(prescriptionMapper).updateById(p);
        }
    }

    // ==================== getDetail ====================

    @Nested
    @DisplayName("getDetail: 处方详情")
    class GetDetail {

        @Test
        @DisplayName("正常返回处方和药材明细")
        void testGetDetail() {
            Prescription p = new Prescription();
            p.setId(1000L);
            p.setPatientName("详情患者");
            p.setRepetition(3);
            when(prescriptionMapper.selectById(1000L)).thenReturn(p);

            PrescriptionMedicine med = new PrescriptionMedicine();
            med.setMedicineName("当归");
            med.setDosage(new BigDecimal("15"));
            when(prescriptionMedicineMapper.selectByPrescriptionId(1000L))
                    .thenReturn(Collections.singletonList(med));

            Prescription result = service.getDetail(1000L);

            assertEquals("详情患者", result.getPatientName());
            assertEquals(Integer.valueOf(3), result.getDoseCount());
            assertEquals(1, result.getMedicineItems().size());
            assertEquals("当归", result.getMedicineItems().get(0).getMedicineName());
        }

        @Test
        @DisplayName("处方不存在时返回null")
        void testGetDetailNotFound() {
            when(prescriptionMapper.selectById(9999L)).thenReturn(null);
            assertNull(service.getDetail(9999L));
        }
    }

    // ==================== list ====================

    @Nested
    @DisplayName("list: 处方列表查询")
    class ListPrescriptions {

        @Test
        @DisplayName("按状态过滤并返回分页结果")
        void testListByStatus() {
            Prescription p = new Prescription();
            p.setId(1L);
            p.setPatientName("患者A");
            p.setReceiveStatus("PENDING");
            p.setRepetition(3);
            when(prescriptionMapper.selectList(any(Wrapper.class)))
                    .thenReturn(Collections.singletonList(p));

            IPage<Prescription> result = service.list(null, null, "PENDING", null, null, null, 1, 10);

            assertEquals(1, result.getTotal());
            assertEquals("PENDING", result.getRecords().get(0).getStatus());
        }
    }

    // ==================== listExceptions ====================

    @Nested
    @DisplayName("listExceptions: 异常处方列表")
    class ListExceptions {

        @Test
        @DisplayName("只返回标记为异常的处方")
        void testListExceptions() {
            Prescription ep = new Prescription();
            ep.setId(2L);
            ep.setPatientName("异常患者");
            ep.setImportException(1);
            when(prescriptionMapper.selectList(any(Wrapper.class)))
                    .thenReturn(Collections.singletonList(ep));

            IPage<Prescription> result = service.listExceptions(1, 10);

            assertEquals(1, result.getTotal());
            assertEquals("异常患者", result.getRecords().get(0).getPatientName());
        }
    }

    // ==================== CSV工具方法 ====================

    @Nested
    @DisplayName("CSV解析：parseCsvLine")
    class CsvParsing {

        @Test
        @DisplayName("解析带引号的CSV行")
        void testParseCsvLineWithQuotes() {
            when(prescriptionConfig.isStrictMode()).thenReturn(false);
            when(prescriptionMapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
                Prescription p = invocation.getArgument(0);
                p.setId(1L);
                return 1;
            });
            when(prescriptionMedicineMapper.selectByPrescriptionId(anyLong()))
                    .thenReturn(Collections.emptyList());

            String csv = "患者姓名,药材名称,用量,单位,备注\n"
                    + "患者A,当归,15,g,\"先煎,后下\"\n";

            List<Prescription> result = service.importFromCsv(csv, null, 1);
            assertEquals(1, result.size());
        }
    }
}
