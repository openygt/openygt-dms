package cn.org.openygt.pda.service;

import cn.org.openygt.pda.entity.PdaOperationLog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

public interface PdaOperationLogService extends IService<PdaOperationLog> {

    PdaOperationLog logOperation(Long userId, String userName, Long deviceId, String deviceCode,
                                 Long taskId, String operType, String operDesc, String operResult,
                                 String apiPath, String httpMethod, String clientIp, Long requestTime);

    List<PdaOperationLog> listRecentByUserId(Long userId, Integer limit);

    List<PdaOperationLog> listByTaskId(Long taskId);

    List<PdaOperationLog> listByTypeAndTimeRange(String operType, LocalDateTime start, LocalDateTime end);
}
