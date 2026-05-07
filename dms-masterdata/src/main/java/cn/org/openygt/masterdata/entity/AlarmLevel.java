package cn.org.openygt.masterdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("md_alarm_level")
public class AlarmLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceId;
    private String name;
    private String color;
    private Integer isUpgrade;
    private Integer upgradeTimes;
    private String alarmMode;
    private Long deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getIsUpgrade() { return isUpgrade; }
    public void setIsUpgrade(Integer isUpgrade) { this.isUpgrade = isUpgrade; }
    public Integer getUpgradeTimes() { return upgradeTimes; }
    public void setUpgradeTimes(Integer upgradeTimes) { this.upgradeTimes = upgradeTimes; }
    public String getAlarmMode() { return alarmMode; }
    public void setAlarmMode(String alarmMode) { this.alarmMode = alarmMode; }
    public Long getDeleted() { return deleted; }
    public void setDeleted(Long deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
