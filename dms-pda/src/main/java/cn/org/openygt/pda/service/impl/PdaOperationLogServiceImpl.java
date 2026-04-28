package cn.org.openygt.pda.service.impl;

import cn.org.openygt.pda.entity.PdaOperationLog;
import cn.org.openygt.pda.mapper.PdaOperationLogMapper;
import cn.org.openygt.pda.service.PdaOperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PdaOperationLogServiceImpl extends ServiceImpl<PdaOperationLogMapper, PdaOperationLog>
        implements PdaOperationLogService {

    @Override
    public PdaOperationLog logOperation(Long userId, String userName, Long deviceId, String deviceCode,
                                        Long taskId, String operType, String operDesc, String operResult,
                                        String apiPath, String httpMethod, String clientIp, Long requestTime) {
        PdaOperationLog log = new PdaOperationLog();
        log.setUserId(userId);
        log.setUserName(userName);
        log.setDeviceId(deviceId);
        log.setDeviceCode(deviceCode);
        log.setTaskId(taskId);
        log.setOperType(operType);
        log.setOperDesc(operDesc);
        log.setOperResult(operResult);
        log.setApiPath(apiPath);
        log.setHttpMethod(httpMethod);
        log.setClientIp(clientIp);
        log.setRequestTime(requestTime);
        log.setOperTime(LocalDateTime.now());
        baseMapper.insert(log);
        return log;
    }

    @Override
    public List<PdaOperationLog> listRecentByUserId(Long userId, Integer limit) {
        return baseMapper.selectRecentByUserId(userId, limit);
    }

    @Override
    public List<PdaOperationLog> listByTaskId(Long taskId) {
        return baseMapper.selectByTaskId(taskId);
    }

    @Override
    public List<PdaOperationLog> listByTypeAndTimeRange(String operType, LocalDateTime start, LocalDateTime end) {
        return baseMapper.selectByTypeAndTimeRange(operType, start, end);
    }
}
