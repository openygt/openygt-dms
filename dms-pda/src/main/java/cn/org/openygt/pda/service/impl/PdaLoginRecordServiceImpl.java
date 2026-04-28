package cn.org.openygt.pda.service.impl;

import cn.org.openygt.pda.entity.PdaLoginRecord;
import cn.org.openygt.pda.mapper.PdaLoginRecordMapper;
import cn.org.openygt.pda.service.PdaLoginRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PdaLoginRecordServiceImpl extends ServiceImpl<PdaLoginRecordMapper, PdaLoginRecord>
        implements PdaLoginRecordService {

    @Override
    @Transactional
    public PdaLoginRecord login(Long userId, String userCode, Long deviceId, String deviceCode, String ipAddress) {
        PdaLoginRecord existing = baseMapper.selectLatestOnlineByUserId(userId);
        if (existing != null) {
            logout(existing.getId());
        }

        PdaLoginRecord record = new PdaLoginRecord();
        record.setUserId(userId);
        record.setUserCode(userCode);
        record.setDeviceId(deviceId);
        record.setDeviceCode(deviceCode);
        record.setLoginTime(LocalDateTime.now());
        record.setStatus("ONLINE");
        record.setIpAddress(ipAddress);
        baseMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public boolean logout(Long recordId) {
        return baseMapper.logoutById(recordId) > 0;
    }

    @Override
    @Transactional
    public boolean logoutByUserId(Long userId) {
        PdaLoginRecord existing = baseMapper.selectLatestOnlineByUserId(userId);
        if (existing != null) {
            return logout(existing.getId());
        }
        return false;
    }

    @Override
    public PdaLoginRecord getLatestOnlineByUserId(Long userId) {
        return baseMapper.selectLatestOnlineByUserId(userId);
    }

    @Override
    public PdaLoginRecord getLatestOnlineByDeviceCode(String deviceCode) {
        return baseMapper.selectLatestOnlineByDeviceCode(deviceCode);
    }

    @Override
    public List<PdaLoginRecord> listAllOnline() {
        return baseMapper.selectAllOnline();
    }
}
