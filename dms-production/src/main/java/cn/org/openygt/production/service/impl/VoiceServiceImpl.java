package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.entity.VoiceSetting;
import cn.org.openygt.production.mapper.VoiceSettingMapper;
import cn.org.openygt.production.service.VoiceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceServiceImpl implements VoiceService {

    private final VoiceSettingMapper voiceSettingMapper;

    @Override
    public String generateTts(String text, String deviceId) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        VoiceSetting setting = getSetting(deviceId);
        if (setting != null && setting.getEnableVoice() != null && setting.getEnableVoice() == 0) {
            return "";
        }
        int repeat = setting != null && setting.getRepeatCount() != null ? setting.getRepeatCount() : 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repeat; i++) {
            sb.append(text);
            if (i < repeat - 1) sb.append(" ");
        }
        return sb.toString();
    }

    @Override
    public VoiceSetting getSetting(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return null;
        }
        return voiceSettingMapper.selectOne(
                new LambdaQueryWrapper<VoiceSetting>().eq(VoiceSetting::getDeviceId, deviceId)
                        .last("LIMIT 1"));
    }

    @Override
    @Transactional
    public VoiceSetting saveSetting(VoiceSetting setting) {
        setting.setCreatedAt(LocalDateTime.now());
        setting.setUpdatedAt(LocalDateTime.now());
        voiceSettingMapper.insert(setting);
        return setting;
    }

    @Override
    @Transactional
    public VoiceSetting updateSetting(Long id, VoiceSetting setting) {
        VoiceSetting existing = voiceSettingMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("设置不存在");
        }
        setting.setId(id);
        setting.setUpdatedAt(LocalDateTime.now());
        voiceSettingMapper.updateById(setting);
        return voiceSettingMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deleteSetting(Long id) {
        voiceSettingMapper.deleteById(id);
    }

    @Override
    public List<VoiceSetting> listSettings() {
        return voiceSettingMapper.selectList(new LambdaQueryWrapper<VoiceSetting>().orderByDesc(VoiceSetting::getCreatedAt));
    }
}
