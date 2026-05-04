package cn.org.openygt.pda.service;

public interface PdaGatewaySessionService {

    void register(String macAddress, String deviceCode, Long userId, String userCode, String userName);

    void unregister(String macAddress);
}
