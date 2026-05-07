package cn.org.openygt.masterdata.service;

import cn.org.openygt.masterdata.entity.Medicine;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface MedicineService {
    Medicine create(Medicine medicine);
    Medicine update(Long id, Medicine medicine);
    Medicine getById(Long id);
    IPage<Medicine> list(String keyword, int page, int size);
    void delete(Long id);
}
