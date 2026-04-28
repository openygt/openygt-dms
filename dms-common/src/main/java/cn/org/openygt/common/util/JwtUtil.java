package cn.org.openygt.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

/**
 * JWT 工具类。
 *
 * <p>基于 jjwt 0.9.1（Java 8 兼容）。</p>
 * <p>支持动态密钥注入，生产环境必须通过 {@link #initSecret} 覆盖。</p>
 */
@Slf4j
public final class JwtUtil {

    private JwtUtil() {
    }

    /**
     * 默认密钥（仅用于开发测试，生产环境必须覆盖）。
     */
    private static String SECRET = "OpenYGT-DMS-JWT-Secret-Key-2024";

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
     * 初始化/覆盖 JWT 密钥。应在应用启动时由配置类调用一次。
     *
     * @param secret 密钥（≥32 字节）
     */
    public static synchronized void initSecret(String secret) {
        if (secret != null && !secret.trim().isEmpty()) {
            SECRET = secret;
            log.info("JWT 密钥已通过配置注入覆盖");
        }
    }

    /**
     * 生成 JWT Token（仅含角色）。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, null, null);
    }

    /**
     * 生成 JWT Token（含角色）。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param roles    角色列表
     * @return JWT 字符串
     */
    public static String generateToken(Long userId, String username, List<String> roles) {
        return generateToken(userId, username, roles, null);
    }

    /**
     * 生成 JWT Token（含角色与权限码）。
     *
     * @param userId      用户 ID
     * @param username    用户名
     * @param roles       角色列表
     * @param permissions 权限码列表
     * @return JWT 字符串
     */
    public static String generateToken(Long userId, String username, List<String> roles, List<String> permissions) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION);
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiry);
        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }
        if (permissions != null && !permissions.isEmpty()) {
            builder.claim("permissions", permissions);
        }
        return builder.signWith(SignatureAlgorithm.HS256, SECRET).compact();
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

    @SuppressWarnings("unchecked")
    public static List<String> getRoles(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("roles", List.class);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getPermissions(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("permissions", List.class);
    }
}
