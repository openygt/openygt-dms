package cn.org.openygt.masterdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("md_medicine")
public class Medicine {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceId;
    private String code;
    private String name;
    private Long categoryId;
    private String unitId;
    private String unitName;
    private BigDecimal salePrice;
    private String model;
    private Integer drugType;
    private Integer isEnabled;
    private String pinyin;
    private String englishName;
    private String medicinalPart;
    private String processingMethod;
    private String fromsFamily;
    private String fromsSpecies;
    private String drugLevel;
    private String efficacyCategory;
    private String mainUsage;
    private String storage;
    private String attention;
    private String drugImg;
    private String remark;
    private String tenantId;
    @TableLogic
    private Long deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getDrugType() { return drugType; }
    public void setDrugType(Integer drugType) { this.drugType = drugType; }
    public Integer getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Integer isEnabled) { this.isEnabled = isEnabled; }
    public String getPinyin() { return pinyin; }
    public void setPinyin(String pinyin) { this.pinyin = pinyin; }
    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }
    public String getMedicinalPart() { return medicinalPart; }
    public void setMedicinalPart(String medicinalPart) { this.medicinalPart = medicinalPart; }
    public String getProcessingMethod() { return processingMethod; }
    public void setProcessingMethod(String processingMethod) { this.processingMethod = processingMethod; }
    public String getFromsFamily() { return fromsFamily; }
    public void setFromsFamily(String fromsFamily) { this.fromsFamily = fromsFamily; }
    public String getFromsSpecies() { return fromsSpecies; }
    public void setFromsSpecies(String fromsSpecies) { this.fromsSpecies = fromsSpecies; }
    public String getDrugLevel() { return drugLevel; }
    public void setDrugLevel(String drugLevel) { this.drugLevel = drugLevel; }
    public String getEfficacyCategory() { return efficacyCategory; }
    public void setEfficacyCategory(String efficacyCategory) { this.efficacyCategory = efficacyCategory; }
    public String getMainUsage() { return mainUsage; }
    public void setMainUsage(String mainUsage) { this.mainUsage = mainUsage; }
    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }
    public String getAttention() { return attention; }
    public void setAttention(String attention) { this.attention = attention; }
    public String getDrugImg() { return drugImg; }
    public void setDrugImg(String drugImg) { this.drugImg = drugImg; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getDeleted() { return deleted; }
    public void setDeleted(Long deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
