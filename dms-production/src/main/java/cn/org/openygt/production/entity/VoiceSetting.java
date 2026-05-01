package cn.org.openygt.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dms_voice_setting")
public class VoiceSetting {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private Long userId;
    private Integer speechRate;
    private Integer volume;
    private String voiceType;
    private Integer enableVoice;
    private String quietStart;
    private String quietEnd;
    private Integer repeatCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
