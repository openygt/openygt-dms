package cn.org.openygt.common.enums;

import java.util.Map;

/**
 * 任务状态展示元数据。
 * <p>统一维护状态的中文显示名、描述模板、颜色和图标，供 PDA / Web 动态获取，避免前端硬编码。</p>
 */
public enum TaskStatusDisplay {

    WAIT_SOAK("待泡药", "等待开始浸泡药材", "#52C41A", "clock"),
    SOAKING("泡药中", "药材正在浸泡，预计{soakTime}分钟后完成", "#722ED1", "water"),
    WAIT_DECOCT("待煎药", "浸泡完成，等待开始煎煮", "#52C41A", "fire"),
    DECOCTING("煎药中", "正在煎煮，预计{decoctTime}分钟后完成", "#F5222D", "fire"),
    WAIT_POUR("待出液", "煎煮完成，等待出液", "#52C41A", "drop"),
    POURING("出液中", "药液正在导出", "#1890FF", "drop"),
    WAIT_WRAP("待包装", "出液完成，等待包装", "#52C41A", "package"),
    WRAPPING("包装中", "正在包装药袋（含贴标）", "#F5222D", "package"),
    WAIT_LABEL("待贴标", "包装完成，等待贴标签", "#FAAD14", "label"),
    WAIT_QC("待质检", "等待质量检验", "#FAAD14", "check"),
    STORED("已暂存", "质检通过，已上架暂存", "#52C41A", "shelf"),
    WAIT_HANDOVER("待交接", "等待取药/配送", "#52C41A", "handover"),
    SECOND_JUDGEMENT("待二次判定", "初判不合格，等待主任二次判定", "#FAAD14", "warning"),
    SUSPENDED("已挂起", "任务暂停中：{suspendReason}", "#FAAD14", "pause"),
    COMPLETED("已完成", "全流程结束", "#52C41A", "success"),
    PARTIAL_COMPLETED("已部分完成", "部分完成，待补交", "#52C41A", "success"),
    SCRAPPED("已报废", "质检不合格已报废", "#CF1322", "error");

    private final String displayName;
    private final String descriptionTemplate;
    private final String color;
    private final String icon;

    TaskStatusDisplay(String displayName, String descriptionTemplate, String color, String icon) {
        this.displayName = displayName;
        this.descriptionTemplate = descriptionTemplate;
        this.color = color;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescriptionTemplate() {
        return descriptionTemplate;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * 渲染描述（替换模板变量）
     */
    public String renderDescription(Map<String, String> params) {
        String result = descriptionTemplate;
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }

    public static TaskStatusDisplay of(TaskStatus status) {
        if (status == null) return null;
        try {
            return valueOf(status.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static TaskStatusDisplay of(String statusLabel) {
        if (statusLabel == null) return null;
        for (TaskStatusDisplay display : values()) {
            if (display.getDisplayName().equals(statusLabel)) {
                return display;
            }
        }
        return null;
    }
}
