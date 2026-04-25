package cn.org.openygt.common.enums;

/**
 * 打印任务状态枚举。
 *
 * <p>替代 dms-print 模块中硬编码的 "PENDING"/"PRINTING"/"COMPLETED"/"FAILED" 字符串，
 * 避免拼写错误导致的运行时问题。</p>
 */
public enum PrintTaskStatus {
    PENDING("待打印"),
    PRINTING("打印中"),
    COMPLETED("已完成"),
    FAILED("失败");

    private final String label;

    PrintTaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
