package cn.org.openygt.rbac.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限注解。
 *
 * <p>标注在 Controller 方法上，要求调用者拥有指定的角色之一。</p>
 * <p>V2.0 使用固定 5 角色模型，不实现数据权限。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermissions {
    /**
     * 所需角色代码列表，满足其一即可。
     */
    String[] value();
}
