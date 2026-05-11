package cn.org.openygt.common.exception;

import cn.org.openygt.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>统一捕获各类异常，转换为标准 ApiResponse 格式返回。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(400, msg);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBind(BindException e) {
        String msg = e.getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(400, msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleIllegalArg(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<?> handleIllegalState(IllegalStateException e) {
        return ApiResponse.error(409, e.getMessage());
    }

    /**
     * 业务异常（第一批 ErrorCode 框架示范）
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常 [{}]: {}", e.getErrorCode().getCode(), e.getMessage());
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleUnauthorized(UnauthorizedException e) {
        return ApiResponse.error(401, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleForbidden(ForbiddenException e) {
        return ApiResponse.error(403, e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleNotFound(ResourceNotFoundException e) {
        return ApiResponse.error(404, e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        return ApiResponse.error(400, "参数类型错误: " + e.getName() + " 的值 " + e.getValue() + " 不是有效的 " + e.getRequiredType().getSimpleName());
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException e) {
        return ApiResponse.error(400, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleMissingPart(org.springframework.web.multipart.support.MissingServletRequestPartException e) {
        return ApiResponse.error(400, "缺少必要上传项: " + e.getRequestPartName());
    }

    @ExceptionHandler(java.lang.IndexOutOfBoundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleIndexOutOfBounds(java.lang.IndexOutOfBoundsException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("fromIndex")) {
            return ApiResponse.error(400, "分页参数错误: page 和 size 必须为正整数");
        }
        return ApiResponse.error(400, "参数范围错误: " + msg);
    }

    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<?> handleDuplicateKey(org.springframework.dao.DuplicateKeyException e) {
        String msg = e.getMessage();
        String friendly = "数据已存在，请检查唯一性约束";
        if (msg != null) {
            // 提取 Duplicate entry 'xxx' for key 'yyy' 中的值和键名
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Duplicate entry '([^']+)' for key '([^']+)'").matcher(msg);
            if (m.find()) {
                String value = m.group(1);
                String key = m.group(2);
                friendly = "「" + value + "」已存在，键「" + key + "」冲突";
            }
        }
        log.warn("唯一约束冲突: {}", friendly);
        return ApiResponse.error(409, friendly);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleGeneric(Exception e) {
        log.error("未处理异常: {} — {}", e.getClass().getName(), e.getMessage(), e);
        return ApiResponse.error(500, "系统繁忙，请稍后重试");
    }
}
