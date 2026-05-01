package cn.org.openygt.quality.mapper;

import cn.org.openygt.quality.entity.InspectionItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspectionItemMapper extends BaseMapper<InspectionItem> {

    @Select("SELECT * FROM qt_inspection_item WHERE inspection_id = #{inspectionId} ORDER BY sort_order, id")
    List<InspectionItem> selectByInspectionId(@Param("inspectionId") Long inspectionId);
}
