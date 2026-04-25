package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.PrintTaskDTO;

import java.util.List;

/**
 * 打印中心模块对外服务接口。
 * 定义在 dms-common，由 dms-print 实现。
 * dms-production 通过注入此接口提交打印任务。
 */
public interface PrintService {

    void submitPrintTask(Long taskId, String deviceCode, String operatorId);

    void retryPrint(Long taskId, String deviceCode, String operatorId);

    List<PrintTaskDTO> getPrintQueue();
}
