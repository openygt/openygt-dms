package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.Doctor;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DoctorService extends IService<Doctor> {

    IPage<Doctor> list(String keyword, Long departmentId, Long hospitalId, int page, int size);

    Doctor create(Doctor doctor);

    Doctor update(Long id, Doctor doctor);

    Doctor getById(Long id);

    void delete(Long id);
}
