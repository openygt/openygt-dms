package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.entity.Doctor;
import cn.org.openygt.masterdata.mapper.DoctorMapper;
import cn.org.openygt.masterdata.service.DoctorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorMapper doctorMapper;

    public DoctorServiceImpl(DoctorMapper doctorMapper) {
        this.doctorMapper = doctorMapper;
    }

    @Override
    @Transactional
    public Doctor create(Doctor doctor) {
        doctorMapper.insert(doctor);
        return doctor;
    }

    @Override
    @Transactional
    public Doctor update(Long id, Doctor doctor) {
        doctor.setId(id);
        doctorMapper.updateById(doctor);
        return doctorMapper.selectById(id);
    }

    @Override
    public Doctor getById(Long id) {
        return doctorMapper.selectById(id);
    }

    @Override
    public IPage<Doctor> list(String keyword, Long hospitalId, int page, int size) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Doctor::getName, keyword)
                    .or().like(Doctor::getSourceId, keyword)
                    .or().like(Doctor::getDepartment, keyword));
        }
        if (hospitalId != null) {
            wrapper.eq(Doctor::getHospitalId, hospitalId);
        }
        wrapper.orderByDesc(Doctor::getCreatedAt);
        return doctorMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        doctorMapper.deleteById(id);
    }
}
