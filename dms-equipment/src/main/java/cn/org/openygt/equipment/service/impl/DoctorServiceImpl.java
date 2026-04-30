package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.Doctor;
import cn.org.openygt.equipment.mapper.DoctorMapper;
import cn.org.openygt.equipment.service.DoctorService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    @Override
    public IPage<Doctor> list(String keyword, Long departmentId, Long hospitalId, int page, int size) {
        QueryWrapper<Doctor> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("doctor_name", keyword)
                    .or().like("doctor_code", keyword));
        }
        if (departmentId != null) {
            wrapper.eq("department_id", departmentId);
        }
        if (hospitalId != null) {
            wrapper.eq("hospital_id", hospitalId);
        }
        wrapper.orderByDesc("created_at");
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public Doctor create(Doctor doctor) {
        if (doctor.getStatus() == null) {
            doctor.setStatus(1);
        }
        baseMapper.insert(doctor);
        return doctor;
    }

    @Override
    @Transactional
    public Doctor update(Long id, Doctor doctor) {
        Doctor existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("医师不存在: " + id);
        }
        doctor.setId(id);
        baseMapper.updateById(doctor);
        return baseMapper.selectById(id);
    }

    @Override
    public Doctor getById(Long id) {
        Doctor doctor = baseMapper.selectById(id);
        if (doctor == null) {
            throw new IllegalArgumentException("医师不存在: " + id);
        }
        return doctor;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        baseMapper.deleteById(id);
    }
}
