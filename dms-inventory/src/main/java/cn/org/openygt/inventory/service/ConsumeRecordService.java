package cn.org.openygt.inventory.service;

import cn.org.openygt.inventory.dto.ConsumeRecordDTO;
import cn.org.openygt.inventory.dto.ConsumeRecordRequest;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface ConsumeRecordService {
    List<Long> recordConsume(ConsumeRecordRequest request);
    List<ConsumeRecordDTO> listByTaskId(Long taskId);
    IPage<ConsumeRecordDTO> pageQuery(Long taskId, Long medicineId, String medicineName, String operatorId, String startTime, String endTime, int page, int size);
}
