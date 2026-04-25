package cn.org.openygt.system.service;

import cn.org.openygt.system.entity.SysLog;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface SysLogService {

    void saveLog(SysLog log);

    IPage<SysLog> list(String keyword, int page, int size);
}
