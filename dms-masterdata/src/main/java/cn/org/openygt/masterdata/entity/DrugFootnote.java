package cn.org.openygt.masterdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("md_drug_footnote")
public class DrugFootnote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String footnoteCode;
    private String footnoteName;
    private Integer sort;
    private Long deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFootnoteCode() { return footnoteCode; }
    public void setFootnoteCode(String footnoteCode) { this.footnoteCode = footnoteCode; }
    public String getFootnoteName() { return footnoteName; }
    public void setFootnoteName(String footnoteName) { this.footnoteName = footnoteName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Long getDeleted() { return deleted; }
    public void setDeleted(Long deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
