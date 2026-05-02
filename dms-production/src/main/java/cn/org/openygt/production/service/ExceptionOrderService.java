package cn.org.openygt.production.service;

import cn.org.openygt.production.entity.ExceptionOrder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface ExceptionOrderService extends IService<ExceptionOrder> {

    ExceptionOrder createOrder(Long taskId, Long deviceId, Integer exceptionType,
                               Integer exceptionLevel, String description);

    ExceptionOrder handleOrder(Long orderId, Long handlerId, String handleResult);

    ExceptionOrder escalateOrder(Long orderId, String escalationReason);

    IPage<ExceptionOrder> listOrders(Integer status, Integer level, Integer exceptionType,
                                      String startDate, String endDate, Integer page, Integer size);

    Map<String, Object> statistics(String groupBy, String startDate, String endDate);
}
