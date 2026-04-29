package cn.org.openygt.production.service;

import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.production.entity.HandoverDetail;
import cn.org.openygt.production.entity.StepLog;
import cn.org.openygt.production.entity.Task;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;
import java.util.List;

public interface TaskService {
    Task bindDevice(Long taskId, String deviceCode);
    IPage<Task> queryTasks(String status, Long deviceId, Long id, Long prescriptionId, String operatorId,
                           String startTime, String endTime, int page, int size);
    Task updateTemperature(String deviceCode, BigDecimal temperature);

    Task startSoak(Long taskId, String operatorId);
    Task endSoak(Long taskId, String operatorId);
    Task startDecoct(Long taskId, String deviceCode, String operatorId);
    Task endDecoct(Long taskId, String operatorId);
    Task startPour(Long taskId, String operatorId);
    Task endPour(Long taskId, String operatorId);
    Task startWrap(Long taskId, String deviceCode, String operatorId);
    Task endWrap(Long taskId, String operatorId);

    Task confirmLabel(Long taskId, String operatorId);
    Task qualityInspect(Long taskId, InspectionResultType result, String operatorId, String remark);
    Task handover(Long taskId, Integer bagCount, String handoverType, String handoverUser, String remark, Boolean isFinal);

    StepLog pauseStep(Long stepLogId, String reason);
    StepLog resumeStep(Long stepLogId);
    List<StepLog> queryStepLogs(Long taskId);
    List<HandoverDetail> queryHandoverDetails(Long taskId);

    Task getById(Long taskId);
    Task getByBarcode(String barcode);
    int clearAll();

    List<Task> queryPrintTasks(String printStatus);
    Task printLabel(Long taskId, String deviceCode, String operatorId);
    Task retryPrint(Long taskId, String deviceCode, String operatorId);

    Task forceStatus(Long taskId, String targetStatus, String operatorId, String deviceCode, String remark);
}
