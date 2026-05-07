package cn.org.openygt.masterdata.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MedicineResponse implements Serializable {
    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String unitName;
    private BigDecimal salePrice;
    private String model;
    private Integer drugType;
    private Integer isEnabled;
    private String pinyin;
    private String englishName;
    private String medicinalPart;
    private String processingMethod;
    private String drugLevel;
    private String efficacyCategory;
    private String mainUsage;
    private String storage;
    private String attention;
    private String drugImg;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
