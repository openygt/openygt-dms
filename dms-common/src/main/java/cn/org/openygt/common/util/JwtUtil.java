package cn.org.openygt.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * JWT 工具类。
 *
 * <p>基于 jjwt 0.9.1（Java 8 兼容）。</p>
 */
@Slf4j
public final class JwtUtil {

    private JwtUtil() {
    }

    /**
     * 默认密钥（MVP 阶段硬编码，生产环境应通过环境变量注入）。
     * 长度 46 字节，满足 HS256 最低 32 字节要求。
     */
    private static final String SECRET = "OpenYGT-DMS-JWT-Secret-Key-2024";

    /**
     * Token 有效期：24 小时（毫秒）。
     */
    private static final long EXPIRATION = 86400000L;

    static {
        // 启动时检查默认密钥，提醒生产环境必须覆盖
        if (SECRET.contains("default") || SECRET.contains("Default") || SECRET.length() < 32) {
            log.warn("⚠️  JWT 使用默认/弱密钥，生产环境必须在 application.yml 中覆盖 jwt.secret 为随机字符串（≥32 字节）！");
        }
    }

    /**
     * 生成 JWT Token。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    /**
     * 解析 Token，返回 Claims。
     *
     * @param token JWT 字符串
     * @return Claims，解析失败返回 null
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效。
     *
     * @param token JWT 字符串
     * @return true 表示有效
     */
    public static boolean validateToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        return claims.getExpiration().after(new Date());
    }

    /**
     * 从 Token 中提取用户 ID。
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从 Token 中提取用户名。
     *
     * @param token JWT 字符串
     * @return 用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("username", String.class);
    }
}
