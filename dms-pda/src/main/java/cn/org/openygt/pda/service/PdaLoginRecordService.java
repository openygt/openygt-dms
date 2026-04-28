package cn.org.openygt.pda.service;

import cn.org.openygt.pda.entity.PdaLoginRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PdaLoginRecordService extends IService<PdaLoginRecord> {

    PdaLoginRecord login(Long userId, String userCode, Long deviceId, String deviceCode, String ipAddress);

    boolean logout(Long recordId);

    boolean logoutByUserId(Long userId);

    PdaLoginRecord getLatestOnlineByUserId(Long userId);

    PdaLoginRecord getLatestOnlineByDeviceCode(String deviceCode);

    List<PdaLoginRecord> listAllOnline();
}
