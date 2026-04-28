package cn.org.openygt.config;

import cn.org.openygt.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * JWT 配置类。
 *
 * <p>负责在应用启动时将配置的密钥注入 {@link JwtUtil}，覆盖默认值。</p>
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        JwtUtil.initSecret(jwtSecret);
    }
}
