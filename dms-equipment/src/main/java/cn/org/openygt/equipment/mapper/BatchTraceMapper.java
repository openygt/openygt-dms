package cn.org.openygt.equipment.mapper;

import cn.org.openygt.equipment.entity.DecoctionTrace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BatchTraceMapper {

    @Select("<script>"
            + "SELECT * FROM decoction_trace WHERE deleted = 0"
            + "<if test='batchNo != null and batchNo !=\"\"'> AND batch_no LIKE CONCAT('%',#{batchNo},'%')</if>"
            + "<if test='prescriptionNo != null and prescriptionNo !=\"\"'> AND prescription_no LIKE CONCAT('%',#{prescriptionNo},'%')</if>"
            + " ORDER BY created_at DESC LIMIT #{offset},#{size}"
            + "</script>")
    List<DecoctionTrace> search(@Param("batchNo") String batchNo, @Param("prescriptionNo") String prescriptionNo,
                                 @Param("offset") int offset, @Param("size") int size);

    @Select("<script>"
            + "SELECT COUNT(*) FROM decoction_trace WHERE deleted = 0"
            + "<if test='batchNo != null and batchNo !=\"\"'> AND batch_no LIKE CONCAT('%',#{batchNo},'%')</if>"
            + "<if test='prescriptionNo != null and prescriptionNo !=\"\"'> AND prescription_no LIKE CONCAT('%',#{prescriptionNo},'%')</if>"
            + "</script>")
    long searchCount(@Param("batchNo") String batchNo, @Param("prescriptionNo") String prescriptionNo);

    @Select("SELECT * FROM decoction_trace WHERE deleted = 0 AND batch_no = #{batchNo} ORDER BY created_at DESC")
    List<DecoctionTrace> findByBatchNo(@Param("batchNo") String batchNo);

    @Select("SELECT DISTINCT batch_no FROM decoction_trace WHERE deleted = 0 AND batch_no IS NOT NULL AND batch_no != '' ORDER BY batch_no DESC LIMIT 50")
    List<String> findBatchNos();
}
