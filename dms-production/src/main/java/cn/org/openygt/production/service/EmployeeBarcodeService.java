package cn.org.openygt.production.service;

import cn.org.openygt.production.entity.EmployeeBarcode;

public interface EmployeeBarcodeService {
    EmployeeBarcode getOrCreateBarcode(Long employeeId);
    EmployeeBarcode recordPrint(Long employeeId);
}
