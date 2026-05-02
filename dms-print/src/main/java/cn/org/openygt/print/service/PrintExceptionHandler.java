package cn.org.openygt.print.service;

import cn.org.openygt.common.service.PrintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 打印失败人工介入处理器
 * <p>注：ExceptionOrderService 在 dms-production 模块，此处通过事件或SPI解耦调用。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrintExceptionHandler {

    private final PrintService printService;

    public void onPrintFailed(Long printTaskId, Long taskId) {
        log.error("打印任务失败，进入人工介入流程: printTaskId={}, taskId={}", printTaskId, taskId);
        // 实际生产环境：通过 ApplicationEventPublisher 发布事件，由 dms-production 监听创建异常工单
        // eventPublisher.publishEvent(new PrintFailedEvent(printTaskId, taskId));
    }

    public void manualReprint(Long taskId, Long printerId, String operatorId) {
        log.info("手动重新打印: taskId={}, printerId={}", taskId, printerId);
        printService.retryPrint(taskId, String.valueOf(printerId), operatorId);
    }
}
