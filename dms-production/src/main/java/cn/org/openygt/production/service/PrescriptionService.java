package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.*;
import cn.org.openygt.production.entity.Prescription;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface PrescriptionService {

    /** 兼容原有文本录入 */
    Prescription create(PrescriptionCreateRequest request);

    /** 结构化录入（手工/导入/OCR共用） */
    Prescription createStructured(PrescriptionStructuredCreateRequest request);

    Prescription getById(Long id);

    IPage<Prescription> list(Long hospitalId, Integer patientType, String status,
                            String keyword, String startTime, String endTime,
                            int page, int size);

    Prescription receive(Long id, Long operatorId, String operatorName);

    Prescription reject(Long id, String rejectType, String reason,
                        Long operatorId, String operatorName);

    Prescription getDetail(Long id);

    /** CSV导入（含批次去重） */
    List<Prescription> importFromCsv(String csvContent, Long hospitalId,
                                     Integer defaultRepetition);

    /** HIS推送接收（含幂等） */
    Prescription createFromPush(PrescriptionPushRequest.PushPrescription push,
                                String hospitalCode);

    /** OCR确认创建 */
    Prescription createFromOcr(OcrPrescriptionRequest request);

    // ==================== 异常处方管理 ====================

    /** 查询异常处方列表 */
    IPage<Prescription> listExceptions(int page, int size);

    /** 纠正异常处方 */
    Prescription resolveException(Long id, PrescriptionStructuredCreateRequest correctedData);
}
