package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.DecoctionTrace;
import cn.org.openygt.equipment.entity.DecoctionTraceEvent;
import cn.org.openygt.equipment.mapper.DecoctionTraceEventMapper;
import cn.org.openygt.equipment.mapper.DecoctionTraceMapper;
import cn.org.openygt.equipment.service.DecoctionTraceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DecoctionTraceServiceImpl implements DecoctionTraceService {

    private final DecoctionTraceMapper traceMapper;
    private final DecoctionTraceEventMapper eventMapper;

    @Override
    @Transactional
    public DecoctionTrace createTrace(DecoctionTrace trace) {
        trace.setStatus("PENDING");
        trace.setLabelPrintCount(0);
        trace.setCreatedAt(LocalDateTime.now());
        trace.setUpdatedAt(LocalDateTime.now());
        traceMapper.insert(trace);
        log.info("追溯记录已创建: prescriptionNo={}", trace.getPrescriptionNo());
        return trace;
    }

    @Override
    public DecoctionTrace getByPrescriptionNo(String prescriptionNo) {
        return traceMapper.findByPrescriptionNo(prescriptionNo);
    }

    @Override
    public Page<DecoctionTrace> queryTraces(String prescriptionNo, String patientName, String status,
                                             String startTime, String endTime, String deviceCode,
                                             long page, long size) {
        QueryWrapper<DecoctionTrace> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (prescriptionNo != null && !prescriptionNo.isEmpty()) {
            wrapper.like("prescription_no", prescriptionNo);
        }
        if (patientName != null && !patientName.isEmpty()) {
            wrapper.like("patient_name", patientName);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge("created_at", startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le("created_at", endTime);
        }
        if (deviceCode != null && !deviceCode.isEmpty()) {
            wrapper.eq("decoct_device_code", deviceCode);
        }
        wrapper.orderByDesc("created_at");
        return traceMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<DecoctionTraceEvent> getTraceEvents(String prescriptionNo) {
        return eventMapper.findByPrescriptionNo(prescriptionNo);
    }

    @Override
    @Transactional
    public DecoctionTrace updateStep(String prescriptionNo, String stepCode, Map<String, Object> data) {
        DecoctionTrace trace = traceMapper.findByPrescriptionNo(prescriptionNo);
        if (trace == null) {
            throw new IllegalArgumentException("追溯记录不存在: " + prescriptionNo);
        }

        String operatorName = data.get("operatorName") != null ? data.get("operatorName").toString() : null;
        LocalDateTime eventTime = data.get("eventTime") != null ? LocalDateTime.parse(data.get("eventTime").toString()) : LocalDateTime.now();

        switch (stepCode) {
            case "RECEIVE":
                trace.setReceiveTime(eventTime);
                trace.setReceiveOperator(operatorName);
                trace.setStatus("RECEIVED");
                break;
            case "AUDIT":
                trace.setAuditTime(eventTime);
                trace.setAuditPassTime(eventTime);
                trace.setAuditOperator(operatorName);
                trace.setStatus("AUDIT_PASS");
                break;
            case "DISPENSE":
                trace.setDispenseTime(eventTime);
                trace.setDispenseOperator(operatorName);
                trace.setStatus("DISPENSED");
                break;
            case "REVIEW":
                trace.setReviewTime(eventTime);
                trace.setReviewOperator(operatorName);
                trace.setStatus("REVIEWED");
                break;
            case "SOAK_START":
                trace.setSoakStartTime(eventTime);
                trace.setSoakOperator(operatorName);
                trace.setStatus("SOAKING");
                break;
            case "SOAK_COMPLETE":
                trace.setSoakEndTime(eventTime);
                break;
            case "FIRST_DECOCT_START":
                trace.setFirstDecoctStart(eventTime);
                trace.setDecoctOperator(operatorName);
                trace.setStatus("FIRST_DECOCTING");
                break;
            case "FIRST_DECOCT_END":
                trace.setFirstDecoctEnd(eventTime);
                break;
            case "SECOND_DECOCT_START":
                trace.setSecondDecoctStart(eventTime);
                trace.setStatus("SECOND_DECOCTING");
                break;
            case "SECOND_DECOCT_END":
                trace.setSecondDecoctEnd(eventTime);
                break;
            case "PACKAGE_START":
                trace.setPackageStartTime(eventTime);
                trace.setPackageOperator(operatorName);
                trace.setStatus("PACKAGING");
                break;
            case "PACKAGE_COMPLETE":
                trace.setPackageEndTime(eventTime);
                break;
            case "DELIVER":
                trace.setDeliverTime(eventTime);
                trace.setDeliverOperator(operatorName);
                trace.setStatus("COMPLETED");
                trace.setCompleteTime(eventTime);
                break;
            default:
                log.warn("未知的步骤编码: {}", stepCode);
        }

        trace.setUpdatedAt(LocalDateTime.now());
        traceMapper.updateById(trace);

        DecoctionTraceEvent event = new DecoctionTraceEvent();
        event.setTraceId(trace.getId());
        event.setPrescriptionNo(prescriptionNo);
        event.setEventCode(stepCode);
        event.setEventName(getEventName(stepCode));
        event.setEventType("MANUAL");
        event.setOperatorName(operatorName);
        event.setEventTime(eventTime);
        event.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(event);

        log.info("追溯步骤更新: {} -> {}, step={}", prescriptionNo, trace.getStatus(), stepCode);
        return trace;
    }

    @Override
    @Transactional
    public void recordEvent(DecoctionTraceEvent event) {
        event.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(event);
        log.info("追溯事件记录: prescriptionNo={}, eventCode={}", event.getPrescriptionNo(), event.getEventCode());
    }

    @Override
    public Map<String, Object> getTemperatureCurve(String prescriptionNo, String granularity) {
        DecoctionTrace trace = traceMapper.findByPrescriptionNo(prescriptionNo);
        if (trace == null || trace.getTempCurveData() == null) {
            Map<String, Object> r = new HashMap<>();
            r.put("data", new ArrayList<>());
            r.put("maxTemp", 0);
            r.put("avgTemp", 0);
            return r;
        }
        Map<String, Object> r = new HashMap<>();
        r.put("data", trace.getTempCurveData());
        r.put("maxTemp", trace.getMaxTemp());
        r.put("avgTemp", trace.getAvgTemp());
        return r;
    }

    @Override
    public Long countByStatusToday(String status) {
        return traceMapper.countByStatusToday(status);
    }

    private String getEventName(String stepCode) {
        Map<String, String> map = new HashMap<>();
        map.put("RECEIVE", "接方");
        map.put("AUDIT", "审方通过");
        map.put("DISPENSE", "调剂完成");
        map.put("REVIEW", "复核通过");
        map.put("SOAK_START", "浸泡开始");
        map.put("SOAK_COMPLETE", "浸泡完成");
        map.put("FIRST_DECOCT_START", "一煎开始");
        map.put("FIRST_DECOCT_END", "一煎完成");
        map.put("SECOND_DECOCT_START", "二煎开始");
        map.put("SECOND_DECOCT_END", "二煎完成");
        map.put("PACKAGE_START", "包装开始");
        map.put("PACKAGE_COMPLETE", "包装完成");
        map.put("DELIVER", "发货");
        return map.getOrDefault(stepCode, stepCode);
    }
}
