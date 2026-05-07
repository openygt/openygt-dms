package cn.org.openygt.common.exception;

import lombok.Getter;

/**
 * 统一错误码枚举
 *
 * <p>采用 int 型编码，与现有 ApiResponse.error(int code, String message) 完全兼容。</p>
 */
@Getter
public enum ErrorCode {

    // ==================== 成功 0 ====================
    SUCCESS(0, "操作成功"),

    // ==================== 系统级错误 1xxx ====================
    SYS_INTERNAL_ERROR(1001, "系统繁忙，请稍后重试"),
    SYS_PARAM_INVALID(1002, "请求参数有误"),
    SYS_REQUEST_TIMEOUT(1003, "请求超时，请重试"),
    SYS_SERVICE_UNAVAILABLE(1004, "服务暂不可用"),

    // ==================== 业务级错误 2xxx ====================
    BIZ_PRESCRIPTION_NOT_FOUND(2001, "处方不存在或已被删除"),
    BIZ_PRESCRIPTION_ALREADY_RECEIVED(2002, "处方已被接收，不能重复操作"),
    BIZ_PRESCRIPTION_STATUS_INVALID(2003, "处方状态不允许此操作"),
    BIZ_TASK_NOT_FOUND(2011, "任务不存在或已被删除"),
    BIZ_TASK_STATUS_INVALID(2012, "当前任务状态不允许此操作"),
    BIZ_TASK_ALREADY_ASSIGNED(2013, "任务已分配，不能重复分配"),
    BIZ_DEVICE_NOT_FOUND(2021, "设备不存在或已被删除"),
    BIZ_DEVICE_OFFLINE(2022, "设备离线，无法执行操作"),
    BIZ_DEVICE_FAULT(2023, "设备故障，请先维修"),

    // ==================== 权限级错误 3xxx ====================
    AUTH_UNAUTHORIZED(3001, "登录已过期，请重新登录"),
    AUTH_FORBIDDEN(3002, "无权执行此操作"),
    AUTH_LOGIN_FAILED(3003, "用户名或密码错误"),

    // ==================== 校验级错误 4xxx ====================
    VALID_FIELD_REQUIRED(4001, "必填项不能为空"),
    VALID_FIELD_FORMAT_INVALID(4002, "字段格式不正确"),
    VALID_FIELD_LENGTH_EXCEEDED(4003, "字段长度超出限制"),
    VALID_FIELD_VALUE_DUPLICATE(4004, "字段值已存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
