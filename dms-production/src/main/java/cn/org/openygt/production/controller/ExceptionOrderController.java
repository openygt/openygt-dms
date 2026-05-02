package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.entity.ExceptionOrder;
import cn.org.openygt.production.service.ExceptionOrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/prod/exception-order")
@RequiredArgsConstructor
public class ExceptionOrderController {

    private final ExceptionOrderService exceptionOrderService;

    @GetMapping("/list")
    public ApiResponse<IPage<ExceptionOrder>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer exceptionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(exceptionOrderService.listOrders(status, level, exceptionType, startDate, endDate, page, size));
    }

    @PostMapping("/{id}/handle")
    public ApiResponse<ExceptionOrder> handle(@PathVariable Long id, @RequestBody HandleRequest request) {
        return ApiResponse.success(exceptionOrderService.handleOrder(id, request.getHandlerId(), request.getHandleResult()));
    }

    @PostMapping("/{id}/escalate")
    public ApiResponse<ExceptionOrder> escalate(@PathVariable Long id, @RequestBody EscalateRequest request) {
        return ApiResponse.success(exceptionOrderService.escalateOrder(id, request.getEscalationReason()));
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics(
            @RequestParam(defaultValue = "TYPE") String groupBy,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(exceptionOrderService.statistics(groupBy, startDate, endDate));
    }

    @lombok.Data
    public static class HandleRequest {
        private Long handlerId;
        private String handleResult;
    }

    @lombok.Data
    public static class EscalateRequest {
        private String escalationReason;
    }
}
