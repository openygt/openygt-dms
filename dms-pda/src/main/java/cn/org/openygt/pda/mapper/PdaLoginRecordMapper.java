package cn.org.openygt.pda.mapper;

import cn.org.openygt.pda.entity.PdaLoginRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PdaLoginRecordMapper extends BaseMapper<PdaLoginRecord> {

    @Select("SELECT * FROM pda_login_record WHERE user_id = #{userId} AND status = 'ONLINE' ORDER BY login_time DESC LIMIT 1")
    PdaLoginRecord selectLatestOnlineByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM pda_login_record WHERE device_code = #{deviceCode} AND status = 'ONLINE' ORDER BY login_time DESC LIMIT 1")
    PdaLoginRecord selectLatestOnlineByDeviceCode(@Param("deviceCode") String deviceCode);

    @Update("UPDATE pda_login_record SET status = 'OFFLINE', logout_time = NOW(), online_duration = TIMESTAMPDIFF(SECOND, login_time, NOW()) WHERE id = #{id}")
    int logoutById(@Param("id") Long id);

    @Select("SELECT * FROM pda_login_record WHERE status = 'ONLINE' ORDER BY login_time DESC")
    List<PdaLoginRecord> selectAllOnline();
}
