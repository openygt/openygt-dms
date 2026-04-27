package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.PrintTaskDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 打印中心模块对外服务接口。
 * 定义在 dms-common，由 dms-print 实现。
 * dms-production 通过注入此接口提交打印任务。
 */
public interface PrintService {

    void submitPrintTask(Long taskId, String deviceCode, String operatorId);

    void retryPrint(Long taskId, String deviceCode, String operatorId);

    /**
     * 查询指定生产任务的最新打印状态。
     *
     * @param taskId 生产任务 ID
     * @return 打印状态（PENDING/PRINTED/FAILED 等），无记录返回 null
     */
    String getPrintStatus(Long taskId);

    List<PrintTaskDTO> getPrintQueue();

    /**
     * 分页查询打印任务列表。
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    IPage<PrintTaskDTO> getPrintTasks(int page, int size);
}
