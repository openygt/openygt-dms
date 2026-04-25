package cn.org.openygt.common.enums;

public enum TaskStatus {
    WAIT_SOAK("待泡药"),
    SOAKING("泡药中"),
    WAIT_DECOCT("待煎药"),
    DECOCTING("煎药中"),
    WAIT_POUR("待出液"),
    POURING("出液中"),
    WAIT_WRAP("待包装"),
    WRAPPING("包装中"),
    WAIT_LABEL("待贴标"),
    WAIT_QC("待质检"),
    WAIT_HANDOVER("待交接"),
    COMPLETED("已完成"),
    PARTIAL_COMPLETED("已部分完成"),
    SCRAPPED("已报废");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
