package cn.org.openygt.production.his;

import cn.org.openygt.production.config.HisConfig;
import cn.org.openygt.production.dto.PrescriptionPushRequest;
import cn.org.openygt.production.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HIS处方拉取定时任务
 *
 * 按配置的 cron 表达式定时从各 HIS 系统拉取新处方。
 * 通过 adapterClass 动态查找对应的 HisAdapter 实现类。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "dms.his", name = "pull-enabled", havingValue = "true")
@RequiredArgsConstructor
public class HisPullScheduler {

    private final HisConfig hisConfig;
    private final PrescriptionService prescriptionService;
    private final List<HisAdapter> adapters;

    /** 各医院上次拉取时间戳 */
    private final Map<String, Long> lastFetchTimeMap = new ConcurrentHashMap<>();

    @Scheduled(cron = "${dms.his.pull-cron:0 */5 * * * *}")
    @SchedulerLock(name = "hisPullScheduler", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public void pullFromHis() {
        if (!hisConfig.isPullEnabled()) {
            log.debug("HIS拉取未启用，跳过");
            return;
        }

        Map<String, HisAdapter> adapterMap = new ConcurrentHashMap<>();
        for (HisAdapter adapter : adapters) {
            adapterMap.put(adapter.getCode(), adapter);
        }

        for (HisConfig.HisAdapter adapterCfg : hisConfig.getAdapters()) {
            try {
                HisAdapter adapter = adapterMap.get(adapterCfg.getCode());
                if (adapter == null) {
                    log.warn("未找到HIS适配器: code={}, class={}", adapterCfg.getCode(), adapterCfg.getAdapterClass());
                    continue;
                }

                long lastFetch = lastFetchTimeMap.getOrDefault(adapterCfg.getCode(), System.currentTimeMillis() - 60000);
                List<PrescriptionPushRequest.PushPrescription> prescriptions =
                        adapter.fetchNewPrescriptions(adapterCfg.getCode(), lastFetch);

                if (prescriptions.isEmpty()) {
                    log.debug("HIS[{}] 无新处方", adapterCfg.getCode());
                } else {
                    log.info("HIS[{}] 获取到 {} 条新处方", adapterCfg.getCode(), prescriptions.size());
                    for (PrescriptionPushRequest.PushPrescription p : prescriptions) {
                        try {
                            prescriptionService.createFromPush(p, adapterCfg.getCode());
                        } catch (Exception e) {
                            log.error("HIS[{}] 处方创建失败: prescriptionNo={}, error={}",
                                    adapterCfg.getCode(), p.getPrescriptionNo(), e.getMessage());
                        }
                    }
                }

                lastFetchTimeMap.put(adapterCfg.getCode(), System.currentTimeMillis());
            } catch (Exception e) {
                log.error("HIS[{}] 拉取出错", adapterCfg.getCode(), e);
            }
        }
    }
}
