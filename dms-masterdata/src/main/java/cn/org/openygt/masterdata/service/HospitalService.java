package cn.org.openygt.masterdata.service;

import cn.org.openygt.masterdata.entity.Hospital;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface HospitalService {

    Hospital create(Hospital hospital);

    Hospital update(Long id, Hospital hospital);

    Hospital getById(Long id);

    IPage<Hospital> list(int page, int size);

    void delete(Long id);
}
