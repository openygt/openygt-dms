package cn.org.openygt.production.his;

import cn.org.openygt.production.dto.PrescriptionPushRequest;

import java.util.List;

/**
 * HIS系统适配器接口
 *
 * 每个HIS系统实现该接口，支持不同的数据格式和协议。
 * 实现类通过 Spring Bean 名称注册，在配置中指定 adapterClass。
 */
public interface HisAdapter {

    /** 适配器唯一编码 */
    String getCode();

    /** 拉取新处方 */
    List<PrescriptionPushRequest.PushPrescription> fetchNewPrescriptions(String hospitalCode, long lastFetchTime);

    /** 将处方状态回传给HIS */
    boolean pushStatusBack(String hospitalCode, String prescriptionNo, String status, String message);
}
