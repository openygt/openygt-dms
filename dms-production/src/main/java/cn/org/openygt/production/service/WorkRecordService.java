package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.EmployeeWorkDailyDTO;
import java.time.LocalDate;
import java.util.List;

public interface WorkRecordService {
    List<EmployeeWorkDailyDTO> getDailyReport(LocalDate date);
}
