package cn.org.openygt.equipment.websocket;

import cn.org.openygt.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT missing or invalid Authorization header");
                throw new IllegalArgumentException("Missing or invalid JWT token");
            }

            token = token.substring(7);
            if (!JwtUtil.validateToken(token)) {
                log.warn("WebSocket CONNECT invalid JWT token");
                throw new IllegalArgumentException("Invalid JWT token");
            }

            Long userId = JwtUtil.getUserId(token);
            String username = JwtUtil.getUsername(token);
            accessor.setUser(() -> String.valueOf(userId));
            accessor.getSessionAttributes().put("userId", userId);
            accessor.getSessionAttributes().put("username", username);
            log.info("WebSocket authenticated: userId={}, username={}", userId, username);
        }

        return message;
    }
}
