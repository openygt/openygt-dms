package cn.org.openygt.masterdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("md_doctor")
public class Doctor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceId;
    private Long hospitalId;
    private String name;
    private Integer role;
    private Integer sex;
    private String mobile;
    private String department;
    private String certificatePractice;
    private String phyQualification;
    private Integer status;
    private String hisDoctorId;
    private Long deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public Integer getSex() { return sex; }
    public void setSex(Integer sex) { this.sex = sex; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCertificatePractice() { return certificatePractice; }
    public void setCertificatePractice(String certificatePractice) { this.certificatePractice = certificatePractice; }
    public String getPhyQualification() { return phyQualification; }
    public void setPhyQualification(String phyQualification) { this.phyQualification = phyQualification; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getHisDoctorId() { return hisDoctorId; }
    public void setHisDoctorId(String hisDoctorId) { this.hisDoctorId = hisDoctorId; }
    public Long getDeleted() { return deleted; }
    public void setDeleted(Long deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
