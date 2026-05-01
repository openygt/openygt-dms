package cn.org.openygt.production.service;

import cn.org.openygt.production.entity.VoiceSetting;

import java.util.List;

public interface VoiceService {
    String generateTts(String text, String deviceId);
    VoiceSetting getSetting(String deviceId);
    VoiceSetting saveSetting(VoiceSetting setting);
    VoiceSetting updateSetting(Long id, VoiceSetting setting);
    void deleteSetting(Long id);
    List<VoiceSetting> listSettings();
}
