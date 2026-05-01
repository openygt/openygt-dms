package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dms_shelf")
public class Shelf {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shelfCode;
    private String shelfName;
    private String areaCode;
    private String areaName;
    private Integer rowNum;
    private Integer layerNum;
    private Integer capacity;
    private Integer currentCount;
    private String shelfType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
