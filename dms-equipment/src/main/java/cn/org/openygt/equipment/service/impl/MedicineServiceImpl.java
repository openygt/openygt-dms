package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.Medicine;
import cn.org.openygt.equipment.mapper.MedicineMapper;
import cn.org.openygt.equipment.service.MedicineService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicineServiceImpl extends ServiceImpl<MedicineMapper, Medicine> implements MedicineService {

    @Override
    public IPage<Medicine> list(String keyword, int page, int size) {
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Medicine::getMedicineName, keyword)
                    .or().like(Medicine::getMedicineCode, keyword)
                    .or().like(Medicine::getAliases, keyword));
        }
        wrapper.orderByDesc(Medicine::getCreatedAt);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public Medicine create(Medicine medicine) {
        if (medicine.getStatus() == null) {
            medicine.setStatus(1);
        }
        // 校验编码唯一性
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Medicine::getMedicineCode, medicine.getMedicineCode());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("药材编码已存在: " + medicine.getMedicineCode());
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
        // 校验编码唯一性（排除自身）
        if (medicine.getMedicineCode() != null && !medicine.getMedicineCode().equals(existing.getMedicineCode())) {
            LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Medicine::getMedicineCode, medicine.getMedicineCode()).ne(Medicine::getId, id);
            if (baseMapper.selectCount(wrapper) > 0) {
                throw new IllegalArgumentException("药材编码已存在: " + medicine.getMedicineCode());
            }
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
        Medicine existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("药材不存在: " + id);
        }
        // 禁用优先，避免物理删除造成处方/库存链路异常
        existing.setStatus(0);
        baseMapper.updateById(existing);
    }
}
