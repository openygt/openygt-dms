package cn.org.openygt.common.exception;

/**
 * 无权限异常（HTTP 403）。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
