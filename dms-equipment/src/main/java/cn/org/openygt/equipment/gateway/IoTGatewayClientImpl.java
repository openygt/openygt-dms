package cn.org.openygt.equipment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * IoT 网关客户端实现
 *
 * <p>使用 RestTemplate 调用 dms-iot-gateway 的指令下发接口。</p>
 */
@Slf4j
@Component
public class IoTGatewayClientImpl implements IoTGatewayClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${iot.gateway.base-url:http://localhost:8090}")
    private String gatewayBaseUrl;

    @Override
    public void sendCommand(String deviceCode, String protocolType, String commandType, Map<String, Object> params) {
        String url = gatewayBaseUrl + "/api/v1/iot/command";

        Map<String, Object> request = new HashMap<>();
        request.put("deviceCode", deviceCode);
        request.put("protocolType", protocolType);
        request.put("commandType", commandType);
        request.put("params", params != null ? params : new HashMap<>());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                if (Boolean.TRUE.equals(success)) {
                    log.info("指令通过网关下发成功: device={}, protocol={}, type={}",
                            deviceCode, protocolType, commandType);
                } else {
                    String error = (String) response.getBody().get("error");
                    log.error("网关返回下发失败: device={}, error={}", deviceCode, error);
                }
            } else {
                log.error("网关下发指令 HTTP 错误: status={}, device={}", response.getStatusCode(), deviceCode);
            }
        } catch (Exception e) {
            log.error("调用网关下发指令异常: device={}, url={}", deviceCode, url, e);
        }
    }
}
