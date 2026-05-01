package cn.org.openygt.production.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 关键操作二次确认注解。
 * <p>标注在Controller方法上，要求前端传入确认标识后方可执行。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfirmOperation {
    /**
     * 操作名称（用于提示语）。
     */
    String value() default "";

    /**
     * 确认提示语，为空时自动生成。
     */
    String message() default "";

    /**
     * 风险级别: low / medium / high。
     */
    String level() default "medium";
}
