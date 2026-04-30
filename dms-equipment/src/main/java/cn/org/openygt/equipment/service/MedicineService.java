package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.Medicine;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MedicineService extends IService<Medicine> {

    IPage<Medicine> list(String keyword, int page, int size);

    Medicine create(Medicine medicine);

    Medicine update(Long id, Medicine medicine);

    Medicine getById(Long id);

    void delete(Long id);
}
