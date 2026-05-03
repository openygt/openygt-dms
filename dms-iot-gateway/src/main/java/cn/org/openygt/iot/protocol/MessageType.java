package cn.org.openygt.iot.protocol;

import java.util.Locale;

public enum MessageType {
    TELEMETRY,
    STATUS,
    ALARM,
    COMMAND_ACK,
    UNKNOWN;

    public static MessageType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        try {
            return MessageType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
