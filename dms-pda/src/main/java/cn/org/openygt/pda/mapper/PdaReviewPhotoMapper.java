package cn.org.openygt.pda.mapper;

import cn.org.openygt.pda.entity.PdaReviewPhoto;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PdaReviewPhotoMapper extends BaseMapper<PdaReviewPhoto> {

    @Select("SELECT * FROM pda_review_photo WHERE task_id = #{taskId} AND deleted = 0 ORDER BY review_time DESC")
    List<PdaReviewPhoto> selectByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM pda_review_photo WHERE prescription_id = #{prescriptionId} AND deleted = 0 ORDER BY review_time DESC")
    List<PdaReviewPhoto> selectByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    @Select("SELECT COUNT(*) FROM pda_review_photo WHERE task_id = #{taskId} AND photo_type = #{photoType} AND deleted = 0")
    Long countByTaskIdAndType(@Param("taskId") Long taskId, @Param("photoType") String photoType);
}
