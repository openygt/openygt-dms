package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.entity.Medicine;
import cn.org.openygt.masterdata.mapper.MdMedicineMapper;
import cn.org.openygt.masterdata.service.MedicineService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("mdMedicineService")
public class MedicineServiceImpl implements MedicineService {

    private final MdMedicineMapper medicineMapper;

    public MedicineServiceImpl(MdMedicineMapper medicineMapper) {
        this.medicineMapper = medicineMapper;
    }

    @Override
    @Transactional
    public Medicine create(Medicine medicine) {
        medicineMapper.insert(medicine);
        return medicine;
    }

    @Override
    @Transactional
    public Medicine update(Long id, Medicine medicine) {
        medicine.setId(id);
        medicineMapper.updateById(medicine);
        return medicineMapper.selectById(id);
    }

    @Override
    public Medicine getById(Long id) {
        return medicineMapper.selectById(id);
    }

    @Override
    public IPage<Medicine> list(String keyword, int page, int size) {
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Medicine::getName, keyword).or().like(Medicine::getCode, keyword).or().like(Medicine::getPinyin, keyword));
        }
        wrapper.orderByDesc(Medicine::getCreatedAt);
        return medicineMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        medicineMapper.deleteById(id);
    }
}
