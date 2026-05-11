package cn.org.openygt.production.service.impl;

import cn.org.openygt.equipment.entity.Medicine;
import cn.org.openygt.equipment.mapper.MedicineMapper;
import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.entity.ToxicMedicine;
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
import cn.org.openygt.common.enums.TaskStatus;
import cn.org.openygt.production.service.PrescriptionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper prescriptionMapper;
    private final TaskMapper taskMapper;
    private final PrescriptionMedicineMapper prescriptionMedicineMapper;
    private final ToxicMedicineService toxicMedicineService;
    private final MedicineMapper medicineMapper;
    private final HospitalMapper hospitalMapper;
    private final PrescriptionConfig prescriptionConfig;
    private final cn.org.openygt.production.mapper.TaskStatusHistoryMapper taskStatusHistoryMapper;

    // ==================== 原有方式兼容 ====================

    @Override
    @Transactional
    public Prescription create(PrescriptionCreateRequest request) {
        Prescription prescription = new Prescription();
        prescription.setPatientName(request.getPatientName());
        prescription.setMedicineList(request.getMedicineList());
        prescription.setRemark(request.getRemark());
        prescription.setReceiveTime(new Date());
        prescription.setReceiveStatus("PENDING");
        prescriptionMapper.insert(prescription);

        Task task = new Task();
        task.setPrescriptionId(prescription.getId());
        task.setStatus(TaskStatus.WAIT_SOAK.getCode());
        task.setTargetTemp(BigDecimal.valueOf(100));
        task.setSoakDuration(30);
        taskMapper.insert(task);
        recordTaskHistory(task.getId(), null, task.getStatus(), "SYSTEM", "处方创建自动生成任务");

        return prescription;
    }

    // ==================== 结构化创建（核心） ====================

    @Override
    @Transactional
    public Prescription createStructured(PrescriptionStructuredCreateRequest request) {
        // STRICT 模式校验
        if (prescriptionConfig.isStrictMode()) {
            validateStrictMode(request.getMedicineItems());
        }
        validateToxicDosages(request.getMedicineItems());

        // 创建处方头
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNumber(request.getPrescriptionNumber());
        prescription.setPatientName(request.getPatientName());
        prescription.setHospitalId(request.getHospitalId());
        prescription.setPatientType(request.getPatientType());
        prescription.setDoctorName(request.getDoctorName());
        prescription.setDepartment(request.getDepartment());
        prescription.setDisease(request.getDisease());
        prescription.setSchemeId(request.getSchemeId());
        prescription.setRepetition(request.getRepetition());
        prescription.setBagsPerRepetition(request.getBagsPerRepetition());
        prescription.setBagCapacity(request.getBagCapacity());
        prescription.setUsageMethod(request.getUsageMethod());
        prescription.setRemark(request.getRemark());
        // 从药材明细拼接药品清单，避免数据库 NOT NULL 约束报错
        String medicineList = request.getMedicineItems().stream()
                .map(item -> item.getMedicineName() + item.getDosage() + (item.getUnit() != null ? item.getUnit() : "g"))
                .collect(Collectors.joining(";"));
        prescription.setMedicineList(medicineList);
        prescription.setReceiveTime(new Date());
        prescription.setReceiveStatus("PENDING");
        prescriptionMapper.insert(prescription);

        // 写入药材明细
        List<PrescriptionMedicineItemRequest> items = request.getMedicineItems();
        for (int i = 0; i < items.size(); i++) {
            insertMedicineItem(prescription.getId(), items.get(i), i);
        }

        // 创建任务
        Task task = new Task();
        task.setPrescriptionId(prescription.getId());
        task.setStatus(TaskStatus.WAIT_SOAK.getCode());
        task.setTargetTemp(BigDecimal.valueOf(100));
        task.setSoakDuration(30);
        if (request.getSchemeId() != null) {
            task.setSchemeId(request.getSchemeId());
        }
        taskMapper.insert(task);
        recordTaskHistory(task.getId(), null, task.getStatus(), "SYSTEM", "结构化处方创建自动生成任务");

        prescription.setMedicineItems(prescriptionMedicineMapper.selectByPrescriptionId(prescription.getId()));
        return prescription;
    }

    // ==================== CSV 导入（带批次去重） ====================

    @Override
    @Transactional
    public List<Prescription> importFromCsv(String csvContent, Long hospitalId, Integer defaultRepetition) {
        List<Prescription> created = new ArrayList<>();
        String[] lines = csvContent.split("\\r?\\n");
        if (lines.length < 2) {
            throw new IllegalArgumentException("CSV文件至少包含表头+一行数据");
        }

        String[] headers = parseCsvLine(lines[0]);
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim().toLowerCase(), i);
        }

        // 按患者名分组
        Map<String, List<String[]>> patientGroups = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] fields = parseCsvLine(line);
            String patientName = getField(fields, colIndex, "患者姓名", "patient_name", "patientname");
            if (patientName == null || patientName.isEmpty()) continue;
            patientGroups.computeIfAbsent(patientName, k -> new ArrayList<>()).add(fields);
        }

        // 构建一个批次签名集合用于去重（同批次内相同患者+相同药材组合视为重复）
        Set<String> batchSignatures = new HashSet<>();

        for (Map.Entry<String, List<String[]>> group : patientGroups.entrySet()) {
            String patientName = group.getKey();

            // 构建该患者的药材签名：药材名+用量 排序后拼接
            List<String> medSignatures = new ArrayList<>();
            for (String[] fields : group.getValue()) {
                String medName = getField(fields, colIndex, "药材名称", "药品名称", "medicine_name", "medicinename");
                String dose = getField(fields, colIndex, "用量", "剂量", "dosage", "dose");
                medSignatures.add((medName != null ? medName : "") + ":" + (dose != null ? dose : "0"));
            }
            Collections.sort(medSignatures);
            String batchSig = patientName + "|" + String.join(",", medSignatures);

            if (batchSignatures.contains(batchSig)) {
                log.warn("CSV导入批次去重：跳过重复处方 [{}]", patientName);
                continue;
            }
            batchSignatures.add(batchSig);

            PrescriptionStructuredCreateRequest req = new PrescriptionStructuredCreateRequest();
            req.setPatientName(patientName);
            req.setHospitalId(hospitalId);
            req.setSource("IMPORT");
            req.setMedicineItems(new ArrayList<>());

            String[] firstRow = group.getValue().get(0);
            String repetitionStr = getField(firstRow, colIndex, "付数", "repetition", "剂数");
            req.setRepetition(repetitionStr != null ? parseInt(repetitionStr, defaultRepetition) : defaultRepetition);
            req.setRemark(getField(firstRow, colIndex, "备注", "remark"));

            int sort = 1;
            for (String[] fields : group.getValue()) {
                PrescriptionMedicineItemRequest med = new PrescriptionMedicineItemRequest();
                med.setMedicineName(getField(fields, colIndex, "药材名称", "药品名称", "medicine_name", "medicinename"));
                String dosageStr = getField(fields, colIndex, "用量", "剂量", "dosage", "dose");
                med.setDosage(parseBigDecimal(dosageStr, BigDecimal.ZERO));
                med.setUnit(getField(fields, colIndex, "单位", "unit"));
                med.setMedUsage(getField(fields, colIndex, "用法", "med_usage", "medusage"));
                med.setSortOrder(sort++);
                req.getMedicineItems().add(med);
            }

            created.add(createStructured(req));
        }

        log.info("CSV导入完成：共创建 {} 条处方", created.size());
        return created;
    }

    // ==================== HIS 推送（带幂等） ====================

    @Override
    @Transactional
    public Prescription createFromPush(PrescriptionPushRequest.PushPrescription push, String hospitalCode) {
        // 1. 解析 hospitalCode → hospitalId
        Long hospitalId = resolveHospitalId(hospitalCode);

        // 2. 幂等检查：同一医院+同一处方号已存在则直接返回
        if (hospitalId != null && push.getPrescriptionNo() != null) {
            Prescription existing = prescriptionMapper.selectOne(
                    new LambdaQueryWrapper<Prescription>()
                            .eq(Prescription::getHospitalId, hospitalId)
                            .eq(Prescription::getPrescriptionNumber, push.getPrescriptionNo())
                            .last("LIMIT 1"));
            if (existing != null) {
                log.info("HIS推送幂等命中：hospitalId={}, prescriptionNo={}, 返回已存在的处方[{}]",
                        hospitalId, push.getPrescriptionNo(), existing.getId());
                existing.setMedicineItems(prescriptionMedicineMapper.selectByPrescriptionId(existing.getId()));
                return existing;
            }
        }

        // 3. 校验 & 构造
        PrescriptionStructuredCreateRequest req = new PrescriptionStructuredCreateRequest();
        req.setPatientName(push.getPatientName());
        req.setPatientType(push.getPatientType());
        req.setDoctorName(push.getDoctorName());
        req.setDepartment(push.getDepartment());
        req.setDisease(push.getDisease());
        req.setRepetition(push.getRepetition());
        req.setUsageMethod(push.getUsageMethod());
        req.setRemark(push.getRemark());
        req.setHospitalId(hospitalId);
        req.setSource("API");
        req.setMedicineItems(new ArrayList<>());

        // 4. 尝试匹配（STRICT模式下匹配失败→异常处方）
        boolean hasException = false;
        StringBuilder exceptionMsg = new StringBuilder();

        int sort = 1;
        for (PrescriptionPushRequest.PushMedicineItem pushItem : push.getItems()) {
            PrescriptionMedicineItemRequest item = new PrescriptionMedicineItemRequest();
            item.setMedicineName(pushItem.getHisMedicineName());
            try {
                item.setDosage(new BigDecimal(pushItem.getDosage()));
            } catch (Exception e) {
                item.setDosage(BigDecimal.ZERO);
            }
            item.setUnit(pushItem.getUnit());
            item.setMedUsage(pushItem.getMedUsage());
            item.setDecoctionMethod(pushItem.getDecoctionMethod());
            item.setSortOrder(sort++);

            if (pushItem.getHisMedicineCode() != null) {
                Medicine matched = medicineMapper.selectOne(
                        new LambdaQueryWrapper<Medicine>()
                                .eq(Medicine::getHisCode, pushItem.getHisMedicineCode())
                                .last("LIMIT 1"));
                if (matched != null) {
                    item.setMedicineId(matched.getId());
                } else if (prescriptionConfig.isStrictMode()) {
                    hasException = true;
                    exceptionMsg.append("药品[").append(pushItem.getHisMedicineName())
                            .append("]未匹配系统目录; ");
                }
            } else if (prescriptionConfig.isStrictMode()) {
                hasException = true;
                exceptionMsg.append("药品[").append(pushItem.getHisMedicineName()).append("]缺少编码; ");
            }

            req.getMedicineItems().add(item);
        }

        // 5. 在 STRICT 模式下校验毒性
        if (!hasException && prescriptionConfig.isStrictMode()) {
            try {
                validateToxicDosages(req.getMedicineItems());
            } catch (IllegalArgumentException e) {
                hasException = true;
                exceptionMsg.append(e.getMessage());
            }
        }

        // 6. 创建处方（异常也创建，标记 importException=1）
        Prescription prescription = new Prescription();
        prescription.setPatientName(req.getPatientName());
        prescription.setHospitalId(req.getHospitalId());
        prescription.setPatientType(req.getPatientType());
        prescription.setDoctorName(req.getDoctorName());
        prescription.setDepartment(req.getDepartment());
        prescription.setDisease(req.getDisease());
        prescription.setRepetition(req.getRepetition());
        prescription.setUsageMethod(req.getUsageMethod());
        prescription.setRemark(req.getRemark());
        prescription.setPrescriptionNumber(push.getPrescriptionNo());
        // 从药材明细拼接药品清单，避免数据库 NOT NULL 约束报错
        String medicineList = req.getMedicineItems().stream()
                .map(item -> item.getMedicineName() + item.getDosage() + (item.getUnit() != null ? item.getUnit() : "g"))
                .collect(Collectors.joining(";"));
        prescription.setMedicineList(medicineList);
        prescription.setReceiveTime(new Date());
        prescription.setReceiveStatus("PENDING");

        if (hasException) {
            prescription.setImportException(1);
            prescription.setExceptionReason(exceptionMsg.toString());
            // 保存原始数据以便人工纠正
            prescription.setRawImportData(rawDataToJson(push, hospitalCode));
        }

        prescriptionMapper.insert(prescription);

        // 写入药材明细（即使异常也写入原始数据）
        for (int i = 0; i < req.getMedicineItems().size(); i++) {
            insertMedicineItem(prescription.getId(), req.getMedicineItems().get(i), i);
        }

        // 任务只在非异常时创建
        if (!hasException) {
            Task task = new Task();
            task.setPrescriptionId(prescription.getId());
            task.setStatus(TaskStatus.WAIT_SOAK.getCode());
            task.setTargetTemp(BigDecimal.valueOf(100));
            task.setSoakDuration(30);
            taskMapper.insert(task);
            recordTaskHistory(task.getId(), null, task.getStatus(), "SYSTEM", "HIS推送自动生成任务");
        }

        log.info("HIS推送处方: hospitalCode={}, prescriptionNo={}, exception={}",
                hospitalCode, push.getPrescriptionNo(), hasException);

        prescription.setMedicineItems(prescriptionMedicineMapper.selectByPrescriptionId(prescription.getId()));
        return prescription;
    }

    // ==================== OCR 确认创建 ====================

    @Override
    @Transactional
    public Prescription createFromOcr(OcrPrescriptionRequest request) {
        PrescriptionStructuredCreateRequest req = new PrescriptionStructuredCreateRequest();
        req.setPatientName(request.getPatientName());
        req.setDoctorName(request.getDoctorName());
        req.setDepartment(request.getDepartment());
        req.setDisease(request.getDisease());
        req.setRepetition(request.getRepetition());
        req.setSource("OCR");
        req.setMedicineItems(new ArrayList<>());

        int sort = 1;
        for (OcrPrescriptionRequest.OcrMedicineItem ocrItem : request.getItems()) {
            PrescriptionMedicineItemRequest item = new PrescriptionMedicineItemRequest();
            item.setMedicineName(ocrItem.getMedicineName());
            item.setDosage(parseBigDecimal(ocrItem.getDosage(), BigDecimal.ZERO));
            item.setUnit(ocrItem.getUnit());
            item.setMedUsage(ocrItem.getMedUsage());
            item.setSortOrder(sort++);

            // 尝试匹配系统药品目录（FLEXIBLE模式可选）
            if (!prescriptionConfig.isStrictMode()) {
                Medicine matched = tryMatchMedicine(ocrItem.getMedicineName());
                if (matched != null) {
                    item.setMedicineId(matched.getId());
                }
            }

            req.getMedicineItems().add(item);
        }

        return createStructured(req);
    }

    // ==================== 异常处方处理 ====================

    @Override
    @Transactional
    public Prescription resolveException(Long id, PrescriptionStructuredCreateRequest correctedData) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new IllegalArgumentException("处方不存在");
        }
        if (!Integer.valueOf(1).equals(prescription.getImportException())) {
            throw new IllegalArgumentException("该处方不是异常处方");
        }

        // 更新处方头信息
        if (correctedData.getPatientName() != null)
            prescription.setPatientName(correctedData.getPatientName());
        if (correctedData.getHospitalId() != null)
            prescription.setHospitalId(correctedData.getHospitalId());
        if (correctedData.getPatientType() != null)
            prescription.setPatientType(correctedData.getPatientType());
        if (correctedData.getDoctorName() != null)
            prescription.setDoctorName(correctedData.getDoctorName());
        if (correctedData.getDepartment() != null)
            prescription.setDepartment(correctedData.getDepartment());
        if (correctedData.getDisease() != null)
            prescription.setDisease(correctedData.getDisease());
        if (correctedData.getSchemeId() != null)
            prescription.setSchemeId(correctedData.getSchemeId());
        if (correctedData.getRepetition() != null)
            prescription.setRepetition(correctedData.getRepetition());
        if (correctedData.getUsageMethod() != null)
            prescription.setUsageMethod(correctedData.getUsageMethod());
        if (correctedData.getRemark() != null)
            prescription.setRemark(correctedData.getRemark());

        // 清除异常标记
        prescription.setImportException(0);
        prescription.setExceptionReason(null);
        prescription.setRawImportData(null);
        prescriptionMapper.updateById(prescription);

        // 删除旧药材明细，写入新明细
        prescriptionMedicineMapper.delete(new LambdaQueryWrapper<PrescriptionMedicine>()
                .eq(PrescriptionMedicine::getPrescriptionId, id));

        if (correctedData.getMedicineItems() != null) {
            for (int i = 0; i < correctedData.getMedicineItems().size(); i++) {
                insertMedicineItem(id, correctedData.getMedicineItems().get(i), i);
            }
        }

        // 如果没有任务，创建一个
        long taskCount = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getPrescriptionId, id));
        if (taskCount == 0) {
            Task task = new Task();
            task.setPrescriptionId(id);
            task.setStatus(TaskStatus.WAIT_SOAK.getCode());
            task.setTargetTemp(BigDecimal.valueOf(100));
            task.setSoakDuration(30);
            taskMapper.insert(task);
            recordTaskHistory(task.getId(), null, task.getStatus(), "SYSTEM", "异常处方纠正自动生成任务");
        }

        log.info("异常处方已纠正: id={}", id);
        return getDetail(id);
    }

    // ==================== 原有方法 ====================

    @Override
    public Prescription getById(Long id) {
        return prescriptionMapper.selectById(id);
    }

    @Override
    public IPage<Prescription> list(Long hospitalId, Integer patientType, String status, String keyword,
                                    String patientName, String prescriptionNumber, String patientPhone,
                                    String startTime, String endTime, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();

        // 默认不显示异常处方（需要单独查询异常列表）
        wrapper.eq(Prescription::getImportException, 0);

        if (hospitalId != null) {
            wrapper.eq(Prescription::getHospitalId, hospitalId);
        }
        if (patientType != null) {
            wrapper.eq(Prescription::getPatientType, patientType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Prescription::getPatientName, keyword)
                    .or().like(Prescription::getPatientPhone, keyword)
                    .or().like(Prescription::getPrescriptionNumber, keyword));
        }
        if (patientName != null && !patientName.isEmpty()) {
            wrapper.like(Prescription::getPatientName, patientName);
        }
        if (prescriptionNumber != null && !prescriptionNumber.isEmpty()) {
            wrapper.like(Prescription::getPrescriptionNumber, prescriptionNumber);
        }
        if (patientPhone != null && !patientPhone.isEmpty()) {
            wrapper.like(Prescription::getPatientPhone, patientPhone);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(Prescription::getCreatedAt, startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(Prescription::getCreatedAt, endTime + " 23:59:59");
        }

        boolean isReceiveStatus = "PENDING".equals(status) || "RECEIVED".equals(status) || "REJECTED".equals(status);
        if (isReceiveStatus) {
            wrapper.eq(Prescription::getReceiveStatus, status);
        }

        wrapper.orderByDesc(Prescription::getCreatedAt);

        List<Prescription> allList = prescriptionMapper.selectList(wrapper);

        // 批量回填医院名称
        Set<Long> hospitalIds = allList.stream().map(Prescription::getHospitalId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!hospitalIds.isEmpty()) {
            List<Hospital> hospitals = hospitalMapper.selectBatchIds(hospitalIds);
            Map<Long, String> hospitalNameMap = hospitals.stream().collect(Collectors.toMap(Hospital::getId, Hospital::getName, (a, b) -> a));
            for (Prescription p : allList) {
                if (p.getHospitalId() != null) {
                    p.setHospitalName(hospitalNameMap.getOrDefault(p.getHospitalId(), ""));
                }
            }
        }
        if (allList.isEmpty()) {
            return new Page<>(page, size);
        }

        Set<Long> prescriptionIds = allList.stream().map(Prescription::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(Task::getPrescriptionId, prescriptionIds);
        List<Task> tasks = taskMapper.selectList(taskWrapper);
        Map<Long, List<Task>> taskMap = tasks.stream().collect(Collectors.groupingBy(Task::getPrescriptionId));

        List<Prescription> filtered = new ArrayList<>();
        for (Prescription p : allList) {
            p.setDoseCount(p.getRepetition());
            p.setDeptName(p.getDepartment());
            if (isReceiveStatus) {
                p.setStatus(p.getReceiveStatus());
                filtered.add(p);
            } else {
                String calcStatus = calcPrescriptionStatus(taskMap.get(p.getId()));
                p.setStatus(calcStatus);
                if (status == null || status.isEmpty() || status.equals(calcStatus)) {
                    filtered.add(p);
                }
            }
        }

        Page<Prescription> resultPage = new Page<>(page, size);
        resultPage.setTotal(filtered.size());
        int from = (page - 1) * size;
        if (from < filtered.size()) {
            int to = Math.min(from + size, filtered.size());
            resultPage.setRecords(filtered.subList(from, to));
        } else {
            resultPage.setRecords(Collections.emptyList());
        }
        return resultPage;
    }

    @Override
    public IPage<Prescription> listExceptions(int page, int size) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getImportException, 1);
        wrapper.orderByDesc(Prescription::getCreatedAt);

        Page<Prescription> resultPage = new Page<>(page, size);
        List<Prescription> records = prescriptionMapper.selectList(wrapper);

        // 手动分页
        resultPage.setTotal(records.size());
        int from = (page - 1) * size;
        if (from < records.size()) {
            int to = Math.min(from + size, records.size());
            resultPage.setRecords(records.subList(from, to));
        } else {
            resultPage.setRecords(Collections.emptyList());
        }
        return resultPage;
    }

    @Override
    @Transactional
    public Prescription receive(Long id, Long operatorId, String operatorName) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new IllegalArgumentException("处方不存在");
        }
        if (!"PENDING".equals(prescription.getReceiveStatus())) {
            throw new IllegalArgumentException("处方状态不是待接收");
        }

        String taskNo = generateTaskNo();
        prescription.setReceiveStatus("RECEIVED");
        prescription.setTaskNo(taskNo);
        prescription.setReceivedAt(new Date());
        prescription.setOperatorId(operatorId);
        prescription.setOperatorName(operatorName);
        prescriptionMapper.updateById(prescription);

        // 异常处方接收时自动创建任务（之前可能因异常未创建）
        long taskCount = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getPrescriptionId, id));
        if (taskCount == 0) {
            Task task = new Task();
            task.setPrescriptionId(id);
            task.setStatus(TaskStatus.WAIT_SOAK.getCode());
            task.setTargetTemp(BigDecimal.valueOf(100));
            task.setSoakDuration(30);
            if (prescription.getSchemeId() != null) {
                task.setSchemeId(prescription.getSchemeId());
            }
            taskMapper.insert(task);
            recordTaskHistory(task.getId(), null, task.getStatus(),
                    operatorId != null ? String.valueOf(operatorId) : "SYSTEM", "处方接收自动生成任务");
        }

        return prescription;
    }

    @Override
    @Transactional
    public Prescription reject(Long id, String rejectType, String reason, Long operatorId, String operatorName) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new IllegalArgumentException("处方不存在");
        }
        if (!"PENDING".equals(prescription.getReceiveStatus())) {
            throw new IllegalArgumentException("处方状态不是待接收");
        }

        prescription.setReceiveStatus("REJECTED");
        prescription.setRejectType(rejectType);
        prescription.setRejectReason(reason);
        prescription.setRejectedAt(new Date());
        prescription.setOperatorId(operatorId);
        prescription.setOperatorName(operatorName);
        prescriptionMapper.updateById(prescription);

        // BUG-23: 处方拒收后连带取消已生成的 task
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getPrescriptionId, id));
        for (Task task : tasks) {
            String oldStatus = task.getStatus();
            if (!cn.org.openygt.common.enums.TaskStatus.CANCELLED.getCode().equals(oldStatus)
                    && !cn.org.openygt.common.enums.TaskStatus.COMPLETED.getCode().equals(oldStatus)
                    && !cn.org.openygt.common.enums.TaskStatus.SCRAPPED.getCode().equals(oldStatus)) {
                task.setStatus(cn.org.openygt.common.enums.TaskStatus.CANCELLED.getCode());
                taskMapper.updateById(task);
                recordTaskHistory(task.getId(), oldStatus, task.getStatus(),
                        operatorId != null ? String.valueOf(operatorId) : "SYSTEM",
                        "处方拒收连带取消");
                log.info("处方拒收连带取消任务: prescriptionId={}, taskId={}, oldStatus={}", id, task.getId(), oldStatus);
            }
        }

        return prescription;
    }

    @Override
    public Prescription getDetail(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            return null;
        }
        prescription.setDoseCount(prescription.getRepetition());
        prescription.setDeptName(prescription.getDepartment());
        List<PrescriptionMedicine> medicines = prescriptionMedicineMapper.selectByPrescriptionId(id);
        prescription.setMedicineItems(medicines);
        if (prescription.getHospitalId() != null) {
            Hospital hospital = hospitalMapper.selectById(prescription.getHospitalId());
            prescription.setHospitalName(hospital != null ? hospital.getName() : "");
        }
        return prescription;
    }

    // ==================== 私有方法 ====================

    private void insertMedicineItem(Long prescriptionId, PrescriptionMedicineItemRequest item, int index) {
        PrescriptionMedicine pm = new PrescriptionMedicine();
        pm.setPrescriptionId(prescriptionId);
        pm.setMedicineId(resolveMedicineId(item));
        pm.setMedicineName(item.getMedicineName());
        pm.setDosage(item.getDosage());
        pm.setUnit(item.getUnit());
        pm.setMedUsage(item.getMedUsage());
        pm.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : index + 1);
        pm.setDecoctionMethod(item.getDecoctionMethod());
        pm.setBatchNo(item.getBatchNo());

        // 尝试填充毒性信息（有目录数据才做）
        if (item.getMedicineId() != null) {
            ToxicMedicine toxic = toxicMedicineService.lambdaQuery()
                    .eq(ToxicMedicine::getMedicineId, item.getMedicineId())
                    .eq(ToxicMedicine::getIsActive, 1)
                    .oneOpt().orElse(null);
            if (toxic != null) {
                pm.setIsToxic(1);
                pm.setToxicityLevel(toxic.getToxicityLevel());
            }
        }

        prescriptionMedicineMapper.insert(pm);
    }

    private void recordTaskHistory(Long taskId, String fromStatus, String toStatus, String operatorId, String remark) {
        cn.org.openygt.production.entity.TaskStatusHistory history = new cn.org.openygt.production.entity.TaskStatusHistory();
        history.setTaskId(taskId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorId(operatorId != null ? operatorId : "SYSTEM");
        history.setOperateTime(java.time.LocalDateTime.now());
        history.setRemark(remark);
        taskStatusHistoryMapper.insert(history);
    }

    private String calcPrescriptionStatus(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return "PENDING";
        boolean allPending = tasks.stream().allMatch(t -> TaskStatus.WAIT_SOAK.getCode().equals(t.getStatus()));
        boolean allCompleted = tasks.stream().allMatch(t -> TaskStatus.COMPLETED.getCode().equals(t.getStatus()));
        if (allPending) return "PENDING";
        if (allCompleted) return "COMPLETED";
        return "PROCESSING";
    }

    private BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String generateTaskNo() {
        String prefix = "D" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Prescription::getTaskNo, prefix)
                .orderByDesc(Prescription::getId)
                .last("LIMIT 1");
        Prescription latest = prescriptionMapper.selectOne(wrapper);
        if (latest != null && latest.getTaskNo() != null && latest.getTaskNo().startsWith(prefix)) {
            try {
                int seq = Integer.parseInt(latest.getTaskNo().substring(prefix.length())) + 1;
                return prefix + String.format("%04d", seq);
            } catch (NumberFormatException e) {
                return prefix + "0001";
            }
        }
        return prefix + "0001";
    }

    /** STRICT 模式下校验：必须关联药品目录 */
    private void validateStrictMode(List<PrescriptionMedicineItemRequest> items) {
        for (PrescriptionMedicineItemRequest item : items) {
            if (item.getMedicineId() == null) {
                throw new IllegalArgumentException(
                        "STRICT模式下药材[" + item.getMedicineName() + "]必须关联系统药品目录");
            }
            Medicine med = medicineMapper.selectById(item.getMedicineId());
            if (med == null) {
                throw new IllegalArgumentException("药材ID[" + item.getMedicineId() + "]不存在于系统药品目录");
            }
        }
    }

    /** 毒性药材用量校验 */
    private void validateToxicDosages(List<PrescriptionMedicineItemRequest> items) {
        if (items == null || items.isEmpty()) return;
        Set<Long> medicineIds = items.stream()
                .map(PrescriptionMedicineItemRequest::getMedicineId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (medicineIds.isEmpty()) return;

        List<ToxicMedicine> toxicList = toxicMedicineService.lambdaQuery()
                .in(ToxicMedicine::getMedicineId, medicineIds)
                .eq(ToxicMedicine::getIsActive, 1)
                .list();

        Map<Long, ToxicMedicine> toxicMap = toxicList.stream()
                .collect(Collectors.toMap(ToxicMedicine::getMedicineId, Function.identity()));

        for (PrescriptionMedicineItemRequest item : items) {
            if (item.getMedicineId() == null || item.getDosage() == null) continue;
            ToxicMedicine toxic = toxicMap.get(item.getMedicineId());
            if (toxic == null || toxic.getMaxDosage() == null) continue;
            if (item.getDosage().compareTo(toxic.getMaxDosage()) > 0) {
                throw new IllegalArgumentException(
                        String.format("药材[%s]单次用量%.2f%s超过最大限制%.2f%s",
                                item.getMedicineName(), item.getDosage(),
                                item.getUnit() != null ? item.getUnit() : "g",
                                toxic.getMaxDosage(), "g"));
            }
        }
    }

    /** hospitalCode → hospitalId */
    private Long resolveHospitalId(String hospitalCode) {
        if (hospitalCode == null || hospitalCode.isEmpty()) return null;
        try {
            Hospital hospital = hospitalMapper.selectOne(
                    new LambdaQueryWrapper<Hospital>()
                            .eq(Hospital::getCode, hospitalCode)
                            .last("LIMIT 1"));
            return hospital != null ? hospital.getId() : null;
        } catch (Exception e) {
            log.warn("解析hospitalCode失败: code={}", hospitalCode, e);
            return null;
        }
    }

    /** 通过药品名称匹配系统目录（精确→别名） */
    private Medicine tryMatchMedicine(String medicineName) {
        if (medicineName == null || medicineName.isEmpty()) return null;
        Medicine exact = medicineMapper.selectOne(
                new LambdaQueryWrapper<Medicine>()
                        .eq(Medicine::getMedicineName, medicineName)
                        .last("LIMIT 1"));
        if (exact != null) return exact;
        return medicineMapper.selectOne(
                new LambdaQueryWrapper<Medicine>()
                        .like(Medicine::getAliases, medicineName)
                        .last("LIMIT 1"));
    }

    /** 解析 medicineId：如果传了就保留，否则尝试匹配 */
    private Long resolveMedicineId(PrescriptionMedicineItemRequest item) {
        if (item.getMedicineId() != null) return item.getMedicineId();
        Medicine matched = tryMatchMedicine(item.getMedicineName());
        return matched != null ? matched.getId() : null;
    }

    /** 原始数据转 JSON 存储 */
    private String rawDataToJson(PrescriptionPushRequest.PushPrescription push, String hospitalCode) {
        try {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("hospitalCode", hospitalCode);
            raw.put("prescriptionNo", push.getPrescriptionNo());
            raw.put("patientName", push.getPatientName());
            raw.put("items", push.getItems());
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(raw);
        } catch (Exception e) {
            return push.getPrescriptionNo();
        }
    }

    // ==================== CSV 解析工具 ====================

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                fields.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString().trim());
        return fields.toArray(new String[0]);
    }

    private String getField(String[] fields, Map<String, Integer> colIndex, String... names) {
        for (String name : names) {
            Integer idx = colIndex.get(name.toLowerCase());
            if (idx != null && idx < fields.length) {
                String val = fields[idx].trim();
                if (!val.isEmpty()) return val;
            }
        }
        return null;
    }

    private Integer parseInt(String str, Integer defaultVal) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
