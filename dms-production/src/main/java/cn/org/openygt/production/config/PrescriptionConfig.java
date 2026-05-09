package cn.org.openygt.production.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dms.prescription")
public class PrescriptionConfig {

    /**
     * 药品目录模式：
     * FLEXIBLE - 药品目录可选，名称和单位自由输入（默认）
     * STRICT   - 必须关联 t_medicine，按目录校验
     */
    private CatalogMode catalogMode = CatalogMode.FLEXIBLE;

    public enum CatalogMode {
        FLEXIBLE,
        STRICT
    }

    public boolean isStrictMode() {
        return CatalogMode.STRICT == catalogMode;
    }
}
