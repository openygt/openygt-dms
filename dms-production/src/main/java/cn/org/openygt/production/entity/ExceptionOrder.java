package cn.org.openygt.production.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 异常工单表（流程驱动）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prod_exception_order")
public class ExceptionOrder extends BaseEntity {

    /** 异常单号 EXC-yyyyMMdd-4位流水 */
    private String exceptionNo;

    /** 关联任务 */
    private Long taskId;

    /** 关联设备 */
    private Long deviceId;

    /** 异常类型: 1=设备故障 2=停电 3=药材缺货 4=操作异常 5=打印失败 6=其他 */
    private Integer exceptionType;

    /** 异常等级: 1=一般 2=严重 3=紧急 */
    private Integer exceptionLevel;

    /** 异常描述 */
    private String description;

    /** 当前状态: 0=待处理 1=处理中 2=已解决 3=已升级 */
    private Integer currentStatus;

    /** 处理人 */
    private Long handlerId;

    /** 处理结果 */
    private String handleResult;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 是否已升级: 0=否 1=是 */
    private Integer escalated;

    /** 升级时间 */
    private LocalDateTime escalateTime;

    /** 关联技术档案ID (prod_exception_log.id) */
    private Long exceptionLogId;
}
