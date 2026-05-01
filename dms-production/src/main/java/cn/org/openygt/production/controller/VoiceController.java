package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.TtsRequest;
import cn.org.openygt.production.entity.VoiceSetting;
import cn.org.openygt.production.service.VoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.org.openygt.production.ProductionModule;

import java.util.List;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping("/tts")
    public ApiResponse<String> tts(@Validated @RequestBody TtsRequest req) {
        return ApiResponse.success(voiceService.generateTts(req.getText(), req.getDeviceId()));
    }

    @GetMapping("/settings")
    public ApiResponse<VoiceSetting> getSettings(@RequestParam(required = false) String deviceId) {
        return ApiResponse.success(voiceService.getSetting(deviceId));
    }

    @GetMapping("/settings/list")
    public ApiResponse<List<VoiceSetting>> listSettings() {
        return ApiResponse.success(voiceService.listSettings());
    }

    @PostMapping("/settings")
    public ApiResponse<VoiceSetting> saveSettings(@Validated @RequestBody VoiceSetting setting) {
        return ApiResponse.success(voiceService.saveSetting(setting));
    }

    @PutMapping("/settings/{id}")
    public ApiResponse<VoiceSetting> updateSettings(@PathVariable Long id, @Validated @RequestBody VoiceSetting setting) {
        return ApiResponse.success(voiceService.updateSetting(id, setting));
    }

    @DeleteMapping("/settings/{id}")
    public ApiResponse<Void> deleteSettings(@PathVariable Long id) {
        voiceService.deleteSetting(id);
        return ApiResponse.success();
    }
}
