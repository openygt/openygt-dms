package cn.org.openygt.production.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dms.his")
public class HisConfig {

    /** 是否启用定时拉取 */
    private boolean pullEnabled = false;

    /** 定时拉取cron表达式 */
    private String pullCron = "0 */5 * * * *";

    /** HIS适配器配置 */
    private List<HisAdapter> adapters = Collections.emptyList();

    @Getter
    @Setter
    public static class HisAdapter {
        private String code;
        private String name;
        private String adapterClass;
        private String baseUrl;
        private String authType;
        private String authConfig;
    }
}
