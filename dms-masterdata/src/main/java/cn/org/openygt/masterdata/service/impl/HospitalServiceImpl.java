package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.mapper.HospitalMapper;
import cn.org.openygt.masterdata.service.HospitalService;
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
    public IPage<Hospital> list(int page, int size) {
        return hospitalMapper.selectPage(new Page<>(page, size), null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        hospitalMapper.deleteById(id);
    }
}
