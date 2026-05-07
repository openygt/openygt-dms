package cn.org.openygt.masterdata.service;

import cn.org.openygt.masterdata.entity.Doctor;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface DoctorService {
    Doctor create(Doctor doctor);
    Doctor update(Long id, Doctor doctor);
    Doctor getById(Long id);
    IPage<Doctor> list(String keyword, Long hospitalId, int page, int size);
    void delete(Long id);
}
