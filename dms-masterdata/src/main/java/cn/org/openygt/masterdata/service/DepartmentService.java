package cn.org.openygt.masterdata.service;

import cn.org.openygt.masterdata.entity.Department;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface DepartmentService {
    Department create(Department department);
    Department update(Long id, Department department);
    Department getById(Long id);
    IPage<Department> list(String keyword, int page, int size);
    void delete(Long id);
}
