package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.dto.GatewayDeviceReportRequest;

public interface GatewayReportService {

    void handleReport(GatewayDeviceReportRequest request);
}
