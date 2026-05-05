package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.mapper.HospitalMapper;
import cn.org.openygt.masterdata.service.HospitalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalServiceImpl implements HospitalService {

    private final HospitalMapper hospitalMapper;

    public HospitalServiceImpl(HospitalMapper hospitalMapper) {
        this.hospitalMapper = hospitalMapper;
    }

    @Override
    @Transactional
    public Hospital create(Hospital hospital) {
        // 校验编码唯一性
        LambdaQueryWrapper<Hospital> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Hospital::getCode, hospital.getCode());
        if (hospitalMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("医院编码已存在: " + hospital.getCode());
        }
        hospitalMapper.insert(hospital);
        return hospital;
    }

    @Override
    @Transactional
    public Hospital update(Long id, Hospital hospital) {
        Hospital existing = hospitalMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("医院不存在: " + id);
        }
        // 校验编码唯一性（排除自身）
        if (hospital.getCode() != null && !hospital.getCode().equals(existing.getCode())) {
            LambdaQueryWrapper<Hospital> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Hospital::getCode, hospital.getCode()).ne(Hospital::getId, id);
            if (hospitalMapper.selectCount(wrapper) > 0) {
                throw new IllegalArgumentException("医院编码已存在: " + hospital.getCode());
            }
        }
        hospital.setId(id);
        hospitalMapper.updateById(hospital);
        return hospitalMapper.selectById(id);
    }

    @Override
    public Hospital getById(Long id) {
        Hospital hospital = hospitalMapper.selectById(id);
        if (hospital == null) {
            throw new IllegalArgumentException("医院不存在: " + id);
        }
        return hospital;
    }

    @Override
    public IPage<Hospital> list(String keyword, int page, int size) {
        LambdaQueryWrapper<Hospital> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Hospital::getName, keyword).or().like(Hospital::getCode, keyword));
        }
        wrapper.orderByDesc(Hospital::getCreatedAt);
        return hospitalMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Hospital existing = hospitalMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("医院不存在: " + id);
        }
        // 禁用优先，避免物理删除造成孤儿数据
        existing.setStatus(0);
        hospitalMapper.updateById(existing);
    }
}
