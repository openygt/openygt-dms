package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.entity.EmployeeBarcode;
import cn.org.openygt.production.mapper.EmployeeBarcodeMapper;
import cn.org.openygt.production.service.EmployeeBarcodeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeBarcodeServiceImpl implements EmployeeBarcodeService {

    private final EmployeeBarcodeMapper employeeBarcodeMapper;

    @Override
    @Transactional
    public EmployeeBarcode getOrCreateBarcode(Long employeeId) {
        EmployeeBarcode barcode = employeeBarcodeMapper.selectOne(
                new LambdaQueryWrapper<EmployeeBarcode>().eq(EmployeeBarcode::getEmployeeId, employeeId)
                        .eq(EmployeeBarcode::getIsActive, 1)
                        .last("LIMIT 1"));
        if (barcode != null) {
            return barcode;
        }
        barcode = new EmployeeBarcode();
        barcode.setEmployeeId(employeeId);
        barcode.setBarcode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        barcode.setBarcodeType("UUID");
        barcode.setCardType("EMPLOYEE");
        barcode.setPrintCount(0);
        barcode.setIsActive(1);
        barcode.setValidFrom(LocalDateTime.now());
        barcode.setValidTo(LocalDateTime.now().plusYears(2));
        barcode.setCreatedAt(LocalDateTime.now());
        employeeBarcodeMapper.insert(barcode);
        return barcode;
    }

    @Override
    @Transactional
    public EmployeeBarcode recordPrint(Long employeeId) {
        EmployeeBarcode barcode = employeeBarcodeMapper.selectOne(
                new LambdaQueryWrapper<EmployeeBarcode>().eq(EmployeeBarcode::getEmployeeId, employeeId)
                        .eq(EmployeeBarcode::getIsActive, 1)
                        .last("LIMIT 1"));
        if (barcode == null) {
            barcode = getOrCreateBarcode(employeeId);
        }
        barcode.setPrintCount(barcode.getPrintCount() != null ? barcode.getPrintCount() + 1 : 1);
        barcode.setLastPrintTime(LocalDateTime.now());
        employeeBarcodeMapper.updateById(barcode);
        return barcode;
    }
}
