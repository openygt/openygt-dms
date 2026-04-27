package cn.org.openygt.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * JWT Token 响应 DTO。
 */
@Data
public class TokenResponse implements Serializable {

    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private Long userId;
    private String username;
    private List<String> roles;
}
