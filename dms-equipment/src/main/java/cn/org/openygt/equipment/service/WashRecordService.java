package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.WashRecord;
import com.baomidou.mybatisplus.extension.service.IService;

public interface WashRecordService extends IService<WashRecord> {

    WashRecord startWash(Long deviceId, Long taskId, Long operatorId);

    WashRecord completeWash(Long washRecordId, Long operatorId, Integer actualDurationMinutes);
}
