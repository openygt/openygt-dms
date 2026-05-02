package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.entity.ExceptionOrder;
import cn.org.openygt.production.mapper.ExceptionOrderMapper;
import cn.org.openygt.production.service.ExceptionOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionOrderServiceImpl extends ServiceImpl<ExceptionOrderMapper, ExceptionOrder>
        implements ExceptionOrderService {

    private final ExceptionOrderMapper exceptionOrderMapper;

    @Override
    @Transactional
    public ExceptionOrder createOrder(Long taskId, Long deviceId, Integer exceptionType,
                                      Integer exceptionLevel, String description) {
        ExceptionOrder order = new ExceptionOrder();
        order.setExceptionNo(generateExceptionNo());
        order.setTaskId(taskId);
        order.setDeviceId(deviceId);
        order.setExceptionType(exceptionType);
        order.setExceptionLevel(exceptionLevel);
        order.setDescription(description);
        order.setCurrentStatus(0);
        order.setEscalated(0);
        exceptionOrderMapper.insert(order);
        log.info("创建异常工单: {}", order.getExceptionNo());
        return order;
    }

    @Override
    @Transactional
    public ExceptionOrder handleOrder(Long orderId, Long handlerId, String handleResult) {
        ExceptionOrder order = exceptionOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("异常工单不存在");
        }
        if (order.getCurrentStatus() != null && order.getCurrentStatus() == 2) {
            throw new IllegalStateException("异常工单已处理");
        }
        order.setHandlerId(handlerId);
        order.setHandleResult(handleResult);
        order.setHandleTime(LocalDateTime.now());
        order.setCurrentStatus(2);
        exceptionOrderMapper.updateById(order);
        return order;
    }

    @Override
    @Transactional
    public ExceptionOrder escalateOrder(Long orderId, String escalationReason) {
        ExceptionOrder order = exceptionOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("异常工单不存在");
        }
        order.setEscalated(1);
        order.setEscalateTime(LocalDateTime.now());
        order.setCurrentStatus(3);
        // 升级等级，最高为3
        int newLevel = Math.min(3, (order.getExceptionLevel() == null ? 1 : order.getExceptionLevel()) + 1);
        order.setExceptionLevel(newLevel);
        exceptionOrderMapper.updateById(order);
        log.warn("异常工单升级: {}, reason={}", order.getExceptionNo(), escalationReason);
        return order;
    }

    @Override
    public IPage<ExceptionOrder> listOrders(Integer status, Integer level, Integer exceptionType,
                                            String startDate, String endDate, Integer page, Integer size) {
        LambdaQueryWrapper<ExceptionOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ExceptionOrder::getCurrentStatus, status);
        }
        if (level != null) {
            wrapper.eq(ExceptionOrder::getExceptionLevel, level);
        }
        if (exceptionType != null) {
            wrapper.eq(ExceptionOrder::getExceptionType, exceptionType);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(ExceptionOrder::getCreatedAt, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(ExceptionOrder::getCreatedAt, endDate);
        }
        wrapper.orderByDesc(ExceptionOrder::getCreatedAt);
        return exceptionOrderMapper.selectPage(new Page<>(page == null ? 1 : page, size == null ? 20 : size), wrapper);
    }

    @Override
    public Map<String, Object> statistics(String groupBy, String startDate, String endDate) {
        LambdaQueryWrapper<ExceptionOrder> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(ExceptionOrder::getCreatedAt, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(ExceptionOrder::getCreatedAt, endDate);
        }
        List<ExceptionOrder> list = exceptionOrderMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        if ("TYPE".equalsIgnoreCase(groupBy)) {
            Map<Integer, Long> byType = new HashMap<>();
            Map<Integer, Long> resolvedByType = new HashMap<>();
            for (ExceptionOrder o : list) {
                byType.merge(o.getExceptionType() == null ? 0 : o.getExceptionType(), 1L, Long::sum);
                if (Integer.valueOf(2).equals(o.getCurrentStatus())) {
                    resolvedByType.merge(o.getExceptionType() == null ? 0 : o.getExceptionType(), 1L, Long::sum);
                }
            }
            result.put("byType", byType);
            result.put("resolvedByType", resolvedByType);
        } else if ("LEVEL".equalsIgnoreCase(groupBy)) {
            Map<Integer, Long> byLevel = new HashMap<>();
            for (ExceptionOrder o : list) {
                byLevel.merge(o.getExceptionLevel() == null ? 1 : o.getExceptionLevel(), 1L, Long::sum);
            }
            result.put("byLevel", byLevel);
        }
        return result;
    }

    private String generateExceptionNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 简化实现：实际生产环境建议使用数据库序列或Redis原子自增
        long count = exceptionOrderMapper.selectCount(
                new LambdaQueryWrapper<ExceptionOrder>()
                        .likeRight(ExceptionOrder::getExceptionNo, "EXC" + dateStr)
        );
        return String.format("EXC%s%04d", dateStr, count + 1);
    }
}
