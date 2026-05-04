package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.service.GatewayDeviceSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GatewayDeviceSessionServiceImpl implements GatewayDeviceSessionService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${iot.gateway.base-url:http://localhost:8090}")
    private String gatewayBaseUrl;

    @Value("${iot.gateway.api-key:}")
    private String gatewayApiKey;

    @Override
    public List<Map<String, Object>> listOnlineManagedDevices() {
        if (gatewayApiKey == null || gatewayApiKey.trim().isEmpty()) {
            log.warn("未配置 iot.gateway.api-key，无法查询网关在线设备映射");
            return Collections.emptyList();
        }

        String url = gatewayBaseUrl + "/api/v1/iot/device-sessions/online-managed";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gateway-Api-Key", gatewayApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null || !Boolean.TRUE.equals(body.get("success"))) {
                log.warn("查询网关在线设备失败: status={}, body={}", response.getStatusCode(), body);
                return Collections.emptyList();
            }
            Object data = body.get("data");
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }
        } catch (Exception e) {
            log.warn("查询网关在线设备异常: url={}", url, e);
        }
        return Collections.emptyList();
    }
}
