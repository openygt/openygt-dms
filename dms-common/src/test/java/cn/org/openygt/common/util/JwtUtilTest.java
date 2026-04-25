package cn.org.openygt.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试。
 */
class JwtUtilTest {

    @Test
    void testGenerateAndParseToken() {
        String token = JwtUtil.generateToken(1L, "admin");
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        Claims claims = JwtUtil.parseToken(token);
        assertNotNull(claims);
        assertEquals("1", claims.getSubject());
        assertEquals("admin", claims.get("username"));
    }

    @Test
    void testValidateValidToken() {
        String token = JwtUtil.generateToken(42L, "user01");
        assertTrue(JwtUtil.validateToken(token));
    }

    @Test
    void testValidateInvalidToken() {
        assertFalse(JwtUtil.validateToken("invalid.token.here"));
        assertFalse(JwtUtil.validateToken(""));
        assertFalse(JwtUtil.validateToken(null));
    }

    @Test
    void testParseInvalidToken() {
        assertNull(JwtUtil.parseToken("bad_token"));
        assertNull(JwtUtil.parseToken(null));
    }

    @Test
    void testGetUserId() {
        String token = JwtUtil.generateToken(99L, "tester");
        assertEquals(Long.valueOf(99L), JwtUtil.getUserId(token));
    }

    @Test
    void testGetUsername() {
        String token = JwtUtil.generateToken(7L, "operator");
        assertEquals("operator", JwtUtil.getUsername(token));
    }

    @Test
    void testGetUserIdFromInvalidToken() {
        assertNull(JwtUtil.getUserId("not.a.token"));
    }

    @Test
    void testGetUsernameFromInvalidToken() {
        assertNull(JwtUtil.getUsername("not.a.token"));
    }
}
