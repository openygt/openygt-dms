package cn.org.openygt.system.service;

import cn.org.openygt.system.entity.InterfaceLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface InterfaceLogService extends IService<InterfaceLog> {

    Page<InterfaceLog> listLogs(Long interfaceId, String result, int page, int size);
}
