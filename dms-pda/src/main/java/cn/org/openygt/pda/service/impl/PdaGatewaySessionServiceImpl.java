package cn.org.openygt.pda.service.impl;

import cn.org.openygt.pda.service.PdaGatewaySessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PdaGatewaySessionServiceImpl implements PdaGatewaySessionService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${iot.gateway.base-url:http://localhost:8090}")
    private String gatewayBaseUrl;

    @Override
    public void register(String macAddress, String deviceCode, Long userId, String userCode, String userName) {
        post("/api/v1/iot/device-sessions/register", buildRequest(macAddress, deviceCode, userId, userCode, userName));
    }

    @Override
    public void unregister(String macAddress) {
        Map<String, Object> request = new HashMap<>();
        request.put("macAddress", normalize(macAddress));
        post("/api/v1/iot/device-sessions/unregister", request);
    }

    private void post(String path, Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(gatewayBaseUrl + path, new HttpEntity<>(request, headers), Map.class);
        } catch (Exception e) {
            log.warn("PDA 同步网关设备会话失败: path={}, request={}", path, request, e);
        }
    }

    private Map<String, Object> buildRequest(String macAddress, String deviceCode,
                                             Long userId, String userCode, String userName) {
        Map<String, Object> request = new HashMap<>();
        request.put("macAddress", normalize(macAddress));
        request.put("deviceCode", deviceCode);
        request.put("userId", userId);
        request.put("userCode", userCode);
        request.put("userName", userName);
        return request;
    }

    private String normalize(String macAddress) {
        return macAddress == null ? null : macAddress.trim().toUpperCase();
    }
}
