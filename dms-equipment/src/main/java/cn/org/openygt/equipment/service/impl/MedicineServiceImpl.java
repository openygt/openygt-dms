package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.Medicine;
import cn.org.openygt.equipment.mapper.MedicineMapper;
import cn.org.openygt.equipment.service.MedicineService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicineServiceImpl extends ServiceImpl<MedicineMapper, Medicine> implements MedicineService {

    @Override
    public IPage<Medicine> list(String keyword, int page, int size) {
        QueryWrapper<Medicine> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("medicine_name", keyword)
                    .or().like("medicine_code", keyword)
                    .or().like("aliases", keyword));
        }
        wrapper.orderByDesc("created_at");
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public Medicine create(Medicine medicine) {
        if (medicine.getStatus() == null) {
            medicine.setStatus(1);
        }
        baseMapper.insert(medicine);
        return medicine;
    }

    @Override
    @Transactional
    public Medicine update(Long id, Medicine medicine) {
        Medicine existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("药材不存在: " + id);
        }
        medicine.setId(id);
        baseMapper.updateById(medicine);
        return baseMapper.selectById(id);
    }

    @Override
    public Medicine getById(Long id) {
        Medicine medicine = baseMapper.selectById(id);
        if (medicine == null) {
            throw new IllegalArgumentException("药材不存在: " + id);
        }
        return medicine;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        baseMapper.deleteById(id);
    }
}
