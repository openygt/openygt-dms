package cn.org.openygt.equipment.iot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
public class MessageRouter {

    private final Map<String, Consumer<DeviceMessage>> handlers = new ConcurrentHashMap<>();

    public void registerHandler(String messageType, Consumer<DeviceMessage> handler) {
        handlers.put(messageType, handler);
        log.info("Message handler registered: type={}", messageType);
    }

    public void route(DeviceMessage msg) {
        if (msg == null || msg.getMessageType() == null) {
            log.warn("Invalid device message, skip routing");
            return;
        }
        String type = msg.getMessageType();
        Consumer<DeviceMessage> handler = handlers.get(type);
        if (handler != null) {
            try {
                handler.accept(msg);
                log.debug("Message routed: type={}, device={}", type, msg.getDeviceCode());
            } catch (Exception e) {
                log.error("Message handler error: type={}, device={}", type, msg.getDeviceCode(), e);
            }
        } else {
            log.warn("No handler for message type: {}, device: {}", type, msg.getDeviceCode());
        }
    }

    public boolean hasHandler(String messageType) {
        return handlers.containsKey(messageType);
    }
}
