package cn.org.openygt.production.his;

import cn.org.openygt.production.dto.PrescriptionPushRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 默认HIS适配器（JSON REST协议）
 *
 * 通过 HTTP GET 从 HIS 接口拉取处方数据，适用 JSON 协议标准接口。
 * 如果 HIS 接口格式不同，可实现自定义 HisAdapter。
 */
@Slf4j
@Component
public class DefaultHisAdapter implements HisAdapter {

    @Override
    public String getCode() {
        return "DEFAULT_JSON";
    }

    @Override
    public List<PrescriptionPushRequest.PushPrescription> fetchNewPrescriptions(String hospitalCode, long lastFetchTime) {
        log.info("默认适配器拉取处方: hospitalCode={}, lastFetchTime={}", hospitalCode, lastFetchTime);
        // 实际实现需要根据 hospitalCode + lastFetchTime 调用 HIS 接口
        // 解析 JSON 返回 PrescriptionPushRequest.PushPrescription 列表
        // 这里返回空列表，具体 HIS 对接需要子类实现或替换
        return Collections.emptyList();
    }

    @Override
    public boolean pushStatusBack(String hospitalCode, String prescriptionNo, String status, String message) {
        log.info("默认适配器回传状态: hospitalCode={}, prescriptionNo={}, status={}", hospitalCode, prescriptionNo, status);
        return true;
    }
}
