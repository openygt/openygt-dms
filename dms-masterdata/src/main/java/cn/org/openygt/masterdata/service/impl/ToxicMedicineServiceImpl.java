package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.entity.ToxicMedicine;
import cn.org.openygt.masterdata.mapper.ToxicMedicineMapper;
import cn.org.openygt.masterdata.service.ToxicMedicineService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ToxicMedicineServiceImpl extends ServiceImpl<ToxicMedicineMapper, ToxicMedicine>
        implements ToxicMedicineService {
}
