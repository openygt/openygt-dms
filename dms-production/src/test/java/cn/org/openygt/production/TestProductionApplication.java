package cn.org.openygt.production;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试专用的 @SpringBootApplication，用于加载 dms-production 模块的 Spring 上下文。
 * 仅扫描本模块的组件，不加载其他模块的 Bean。
 */
@SpringBootApplication(scanBasePackages = "cn.org.openygt.production")
public class TestProductionApplication {
}
