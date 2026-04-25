package cn.org.openygt.common.exception;

/**
 * 未认证异常（HTTP 401）。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
