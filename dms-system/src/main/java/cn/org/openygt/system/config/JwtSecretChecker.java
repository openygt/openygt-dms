package cn.org.openygt.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * JWT 密钥强度检测。
 *
 * <p>启动时检测：密钥为空、含"Test"、长度小于32位，则拒绝启动。</p>
 */
@Component
public class JwtSecretChecker implements CommandLineRunner {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("[JWT] 密钥未配置，请设置环境变量 JWT_SECRET");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("[JWT] 密钥长度不足32位，请设置更强的 JWT_SECRET");
        }
        if (jwtSecret.toLowerCase().contains("test")) {
            throw new IllegalStateException("[JWT] 密钥含有弱密钥标识(Test)，请设置正式 JWT_SECRET");
        }
    }
}
