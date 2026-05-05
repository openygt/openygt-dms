package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.Doctor;
import cn.org.openygt.equipment.mapper.DoctorMapper;
import cn.org.openygt.equipment.service.DoctorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    @Override
    public IPage<Doctor> list(String keyword, Long departmentId, Long hospitalId, int page, int size) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Doctor::getDoctorName, keyword)
                    .or().like(Doctor::getDoctorCode, keyword));
        }
        if (departmentId != null) {
            wrapper.eq(Doctor::getDepartmentId, departmentId);
        }
        if (hospitalId != null) {
            wrapper.eq(Doctor::getHospitalId, hospitalId);
        }
        wrapper.orderByDesc(Doctor::getCreatedAt);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public Doctor create(Doctor doctor) {
        if (doctor.getStatus() == null) {
            doctor.setStatus(1);
        }
        // 校验编码唯一性
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getDoctorCode, doctor.getDoctorCode());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("医师编码已存在: " + doctor.getDoctorCode());
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
        // 校验编码唯一性（排除自身）
        if (doctor.getDoctorCode() != null && !doctor.getDoctorCode().equals(existing.getDoctorCode())) {
            LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Doctor::getDoctorCode, doctor.getDoctorCode()).ne(Doctor::getId, id);
            if (baseMapper.selectCount(wrapper) > 0) {
                throw new IllegalArgumentException("医师编码已存在: " + doctor.getDoctorCode());
            }
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
        Doctor existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("医师不存在: " + id);
        }
        // 禁用优先，避免物理删除造成历史数据不可追溯
        existing.setStatus(0);
        baseMapper.updateById(existing);
    }
}
