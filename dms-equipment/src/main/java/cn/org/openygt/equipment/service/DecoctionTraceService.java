package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.DecoctionTrace;
import cn.org.openygt.equipment.entity.DecoctionTraceEvent;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface DecoctionTraceService {

    DecoctionTrace createTrace(DecoctionTrace trace);

    DecoctionTrace getByPrescriptionNo(String prescriptionNo);

    Page<DecoctionTrace> queryTraces(String prescriptionNo, String patientName, String status,
                                      String startTime, String endTime, String deviceCode,
                                      long page, long size);

    List<DecoctionTraceEvent> getTraceEvents(String prescriptionNo);

    DecoctionTrace updateStep(String prescriptionNo, String stepCode, Map<String, Object> data);

    void recordEvent(DecoctionTraceEvent event);

    Map<String, Object> getTemperatureCurve(String prescriptionNo, String granularity);

    Long countByStatusToday(String status);
}
