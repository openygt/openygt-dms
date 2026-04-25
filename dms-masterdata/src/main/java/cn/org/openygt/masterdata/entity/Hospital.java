package cn.org.openygt.masterdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("md_hospital")
public class Hospital {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String tenantId;
    @TableLogic
    private Integer deleted;
    private Date createdAt;
    private Date updatedAt;
}
